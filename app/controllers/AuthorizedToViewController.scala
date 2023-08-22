/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import actionbuilders.{AuthenticatedRequest, IdentifierAction}
import config.{AppConfig, ErrorHandler}
import connectors.{CustomsFinancialsApiConnector, SdesConnector}
import domain.FileRole.StandingAuthority
import domain._
import forms.EoriNumberFormProvider
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.api.{Logger, LoggerLike}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Utils.{CsvFiles, emptyString, isXIEori, partitionCsvFilesByFileNamePattern}
import views.helpers.Formatters
import views.html.authorised_to_view._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorizedToViewController @Inject()(authenticate: IdentifierAction,
                                           apiService: ApiService,
                                           val sdesConnector: SdesConnector,
                                           errorHandler: ErrorHandler,
                                           dataStoreService: DataStoreService,
                                           financialsApiConnector: CustomsFinancialsApiConnector,
                                           implicit val mcc: MessagesControllerComponents,
                                           authorisedToViewSearch: authorised_to_view_search,
                                           authorisedToViewSearchResult: authorised_to_view_search_result,
                                           authorisedToViewSearchNoResult: authorised_to_view_search_no_result,
                                           eoriNumberFormProvider: EoriNumberFormProvider)(
                                           implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)
  val form: Form[String] = eoriNumberFormProvider()

  def onPageLoad(): Action[AnyContent] = authenticate async { implicit req =>
    financialsApiConnector.deleteNotification(req.user.eori, StandingAuthority)

    for {
      csvFiles: Seq[StandingAuthorityFile] <- getCsvFile(req.user.eori)
    } yield {
      val viewModel = csvFiles
      val fileExists = csvFiles.nonEmpty
      val csvFilesForGBAndXI: CsvFiles = partitionCsvFilesByFileNamePattern(viewModel)

      val gbAuthUrl = csvFilesForGBAndXI.gbCsvFiles.headOption.map(_.downloadURL)
      val xiAuthUrl = csvFilesForGBAndXI.xiCsvFiles.headOption.map(_.downloadURL)
      val date = Formatters.dateAsDayMonthAndYear(
        Some(csvFilesForGBAndXI.gbCsvFiles.headOption.map(_.startDate).getOrElse(LocalDate.now)).get)
      val isXiEoriEnabled = appConfig.xiEoriEnabled

      Ok(authorisedToViewSearch(form, gbAuthUrl, xiAuthUrl, date, fileExists, isXiEoriEnabled))
    }
  }

  def onSubmit(): Action[AnyContent] = authenticate async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        for {
          csvFiles <- getCsvFile(request.user.eori)
        } yield {
          val viewModel = csvFiles
          val fileExists = csvFiles.nonEmpty
          val csvFilesForGBAndXI: CsvFiles = partitionCsvFilesByFileNamePattern(viewModel)

          val gbAuthUrl: Option[EORI] = csvFilesForGBAndXI.gbCsvFiles.headOption.map(_.downloadURL)
          val xiAuthUrl = csvFilesForGBAndXI.xiCsvFiles.headOption.map(_.downloadURL)
          val date = Formatters.dateAsDayMonthAndYear(
            Some(csvFilesForGBAndXI.gbCsvFiles.headOption.map(_.startDate).getOrElse(LocalDate.now)).get)
          val isXiEoriEnabled = appConfig.xiEoriEnabled

          BadRequest(authorisedToViewSearch(formWithErrors, gbAuthUrl, xiAuthUrl, date, fileExists, isXiEoriEnabled))
        },
      query => processSearchQuery(request, query)
    )
  }

  private def processSearchQuery(request: AuthenticatedRequest[AnyContent],
                                 query: EORI)(
                                  implicit hc: HeaderCarrier,
                                  messages: Messages, appConfig: AppConfig): Future[Result] = {
    val searchQuery = stripWithWhitespace(query)

    val result = for {
      cdsAccounts: CDSAccounts <- apiService.getAccounts(request.user.eori)
      xiEORI: Option[EORI] <- dataStoreService.getXiEori(request.user.eori)
    } yield {
      val isMyAcc = cdsAccounts.myAccounts.exists(_.number == query)

      (request.user.eori, isMyAcc, xiEORI) match {
        case (eori, _, _) if eori.equalsIgnoreCase(query) =>
          displayErrorView(query,"cf.account.authorized-to-view.search-own-eori")(
            request, messages, appConfig)
        case (_, true, _) =>
          displayErrorView(query,"cf.account.authorized-to-view.search-own-accountnumber")(
            request, messages, appConfig)
        case (_, _, assocXiEori) if assocXiEori.isEmpty && isXIEori(searchQuery) =>
          displayErrorView(query,"cf.search.authorities.error.register-xi-eori")(
            request, messages, appConfig)
        case _ =>
          if(xiEORI.nonEmpty) {
            searchAuthoritiesForValidInput(request, searchQuery, xiEORI)
          } else {
            searchAuthoritiesForValidInput(request, searchQuery)
          }
      }
    }
    result.flatten
  }

  /**
   * Displays the error view for the Business exceptions using
   * relevant message key
   */
  private def displayErrorView(query: EORI,
                               msgKey: String)(
                                implicit request: Request[_],
                                messages: Messages,
                                appConfig: AppConfig): Future[Result] =
    Future.successful(BadRequest(authorisedToViewSearch(
      form.withError("value", msgKey).fill(query),
      None,
      None,
      LocalDate.now.toString,
      fileExists = false,
      isXiEoriEnabled = false)(request, messages, appConfig)))

  /**
   * Search and processes the Authorities for the valid input, further, displays the view accordingly
   */
  private def searchAuthoritiesForValidInput(request: AuthenticatedRequest[AnyContent],
                                             searchQuery: EORI,
                                             xiEORI: Option[String] = None)
                                            (implicit hc: HeaderCarrier,
                                             messages: Messages, appConfig: AppConfig): Future[Result] = {
    val result = for {
      authForGBEORI <- apiService.searchAuthorities(request.user.eori, searchQuery)
      authForXIEORI <- if (xiEORI.isDefined) {
        apiService.searchAuthorities(xiEORI.getOrElse(emptyString), searchQuery)
      } else {
        Future.successful(Left(NoAuthorities))
      }
    } yield {
      (authForGBEORI, authForXIEORI) match {
        case (Left(NoAuthorities), Left(NoAuthorities)) =>
          Future.successful(Ok(authorisedToViewSearchNoResult(searchQuery)(request, messages, appConfig)))

        case (Left(SearchError), Left(SearchError)) | (Left(SearchError), Left(NoAuthorities))
             | (Left(NoAuthorities), Left(SearchError)) =>
          Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))

        case (Right(gbAuthorities), Left(_)) =>
          processAuthAndViewResultPage(request, searchQuery, messages, appConfig, gbAuthorities)

        case (Left(_), Right(xiAuthorities)) =>
          processAuthAndViewResultPage(request, searchQuery, messages, appConfig, xiAuthorities, isGBAuth = false)

        case (Right(gbAuthorities), Right(xiAuthorities)) =>
          //Currently below is processing authorities when search string is an account number
          processGBAndXIAuthAndViewResultPage(request, searchQuery, messages, appConfig, gbAuthorities, xiAuthorities)

          //TODO: Need to be looked at
        /*case (Left(SearchError), Left(NoAuthorities)) =>
          if (isSearchQueryAnAccountNumber(searchQuery)) {
            Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))
          } else {
            Future.successful(Ok(authorisedToViewSearchNoResult(searchQuery)(request, messages, appConfig)))
          }*/
      }
    }
    result.flatten
  }

  /**
   * Processes the SearchedAuthorities for GB or XI EORI
   */
  private def processAuthAndViewResultPage(request: AuthenticatedRequest[AnyContent],
                                           searchQuery: EORI,
                                           messages: Messages,
                                           appConfig: AppConfig,
                                           searchedAuthorities: SearchedAuthorities,
                                           isGBAuth: Boolean = true)(implicit hc: HeaderCarrier): Future[Result] = {
    val displayLink: Boolean = getDisplayLink(searchedAuthorities)
    val clientEori: EORI = getClientEori(searchedAuthorities)

    dataStoreService.getCompanyName(clientEori).flatMap {
      companyName => {

        val searchResultView = if (isGBAuth) {
          authorisedToViewSearchResult(
            searchQuery, Option(clientEori), searchedAuthorities, companyName, displayLink)(
            request, messages, appConfig)
        } else {
          authorisedToViewSearchResult(
            searchQuery, None, searchedAuthorities, companyName, displayLink, Option(clientEori))(
            request, messages, appConfig)
        }

        Future.successful(Ok(searchResultView))
      }
    }
  }

  /**
   * Processes the Authorities for both GB and XI EORI. It is only used when
   * search string is a valid account number
   */
  private def processGBAndXIAuthAndViewResultPage(request: AuthenticatedRequest[AnyContent],
                                                  searchQuery: EORI,
                                                  messages: Messages,
                                                  appConfig: AppConfig,
                                                  gbAuthorities: SearchedAuthorities,
                                                  xiAuthorities: SearchedAuthorities)(
                                                   implicit hc: HeaderCarrier): Future[Result] = {
    val displayLinkForGBAuth = getDisplayLink(gbAuthorities)
    val displayLinkForXIAuth = getDisplayLink(xiAuthorities)
    val displayLink = displayLinkForGBAuth && displayLinkForXIAuth

    val gbEori: EORI = getClientEori(gbAuthorities)
    val xiEori: EORI = getClientEori(xiAuthorities)

    dataStoreService.getCompanyName(gbEori).flatMap {
      companyName => {
        Future.successful(Ok(
          authorisedToViewSearchResult(
            searchQuery,
            Option(gbEori),
            finalSearchAuthoritiesToShow(gbAuthorities, xiAuthorities),
            companyName,
            displayLink,
            Option(xiEori))(request, messages, appConfig)))
      }
    }
  }

  /**
   * Selects the SearchedAuthorities (out of GB and XI authorities) which has permission to show the balance.
   * Selects gbAuthorities when both authorities have no balance
   *
   * It is used only when search string is a valid account number
   */
  private def finalSearchAuthoritiesToShow(gbAuthorities: SearchedAuthorities,
                                           xiAuthorities: SearchedAuthorities): SearchedAuthorities = {
    val listOfEligibleAuthorities = List(gbAuthorities, xiAuthorities).filter(sAuth => !getDisplayLink(sAuth))

    if (listOfEligibleAuthorities.isEmpty) gbAuthorities else listOfEligibleAuthorities.head
  }

  private def getCsvFile(eori: String)(implicit req: AuthenticatedRequest[_]): Future[Seq[StandingAuthorityFile]] = {
    sdesConnector.getAuthoritiesCsvFiles(eori)
      .map(_.sortWith(_.startDate isAfter _.startDate).sortBy(_.filename).toSeq.sortWith(_.filename > _.filename))
  }

  private def getClientEori(searchedAuthorities: SearchedAuthorities) = {
    searchedAuthorities.authorities.map {
      case AuthorisedDutyDefermentAccount(account, balances) => account.accountOwner
      case AuthorisedCashAccount(account, availableAccountBalance) => account.accountOwner
      case AuthorisedGeneralGuaranteeAccount(account, availableGuaranteeBalance) => account.accountOwner
    }.head
  }

  private def getDisplayLink(searchedAuthorities: SearchedAuthorities) = {
    searchedAuthorities.authorities.exists {
      case AuthorisedDutyDefermentAccount(account, balances) => balances.map(_.periodAvailableAccountBalance).isEmpty
      case AuthorisedCashAccount(account, availableAccountBalance) => availableAccountBalance.isEmpty
      case AuthorisedGeneralGuaranteeAccount(account, availableGuaranteeBalance) => availableGuaranteeBalance.isEmpty
    }
  }

  protected def stripWithWhitespace(str: String): String =
    str.replaceAll("\\s", emptyString).toUpperCase
}

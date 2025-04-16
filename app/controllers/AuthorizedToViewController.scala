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

import actionbuilders.{AuthenticatedRequest, EmailAction, IdentifierAction}
import config.{AppConfig, ErrorHandler}
import connectors.{CustomsFinancialsApiConnector, SdesConnector}
import domain.FileRole.StandingAuthority
import domain._
import forms.EoriNumberFormProvider
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import play.api.{Logger, LoggerLike}
import repositories.QueryCacheRepository
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Utils.{CsvFiles, emptyString, isXIEori, partitionCsvFilesByFileNamePattern}
import views.helpers.Formatters
import views.html.authorised_to_view._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off cyclomatic.complexity
@Singleton
class AuthorizedToViewController @Inject() (
  authenticate: IdentifierAction,
  apiService: ApiService,
  val sdesConnector: SdesConnector,
  errorHandler: ErrorHandler,
  dataStoreService: DataStoreService,
  verifyEmail: EmailAction,
  financialsApiConnector: CustomsFinancialsApiConnector,
  queryRepository: QueryCacheRepository,
  implicit val mcc: MessagesControllerComponents,
  authorisedToViewSearch: authorised_to_view_search,
  authorisedToViewSearchResult: authorised_to_view_search_result,
  authorisedToViewSearchNoResult: authorised_to_view_search_no_result,
  eoriNumberFormProvider: EoriNumberFormProvider
)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  val log: LoggerLike    = Logger(this.getClass)
  val form: Form[String] = eoriNumberFormProvider(appConfig.isEUEoriEnabled)

  def onPageLoad(): Action[AnyContent] = authenticate andThen verifyEmail async { implicit req =>
    financialsApiConnector.deleteNotification(StandingAuthority)

    getCsvFile().map { csvFiles =>
      val isNotificationPanelEnabled = appConfig.isAuthoritiesNotificationPanelEnabled

      val (authUrl, xiAuthUrl, date, fileExists) =
        if (isNotificationPanelEnabled) {
          val csvFilesForEoris: CsvFiles = partitionCsvFilesByFileNamePattern(csvFiles)

          val authUrlOpt    = csvFilesForEoris.csvFiles.headOption.map(_.downloadURL)
          val xiAuthUrlOpt  = csvFilesForEoris.xiCsvFiles.headOption.map(_.downloadURL)
          val dateOpt       = Formatters.dateAsDayMonthAndYear(
            Some(csvFilesForEoris.csvFiles.headOption.map(_.startDate).getOrElse(LocalDate.now)).get
          )
          val fileExistsOpt = csvFiles.nonEmpty

          (authUrlOpt, xiAuthUrlOpt, Some(dateOpt), Some(fileExistsOpt))
        } else {
          (None, None, None, None)
        }

      Ok(
        authorisedToViewSearch(
          form,
          authUrl,
          xiAuthUrl,
          date,
          fileExists,
          appConfig.xiEoriEnabled,
          appConfig.isEUEoriEnabled,
          isNotificationPanelEnabled
        )
      )
    }
  }

  def onSubmit(): Action[AnyContent] = authenticate async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getCsvFile().map { csvFiles =>
            val isAuthoritiesNotificationPanelEnabled = appConfig.isAuthoritiesNotificationPanelEnabled

            val (authUrl, xiAuthUrl, date, fileExists) =
              if (isAuthoritiesNotificationPanelEnabled) {

                val csvFilesForEoris: CsvFiles = partitionCsvFilesByFileNamePattern(csvFiles)

                val authUrlOpt    = csvFilesForEoris.csvFiles.headOption.map(_.downloadURL)
                val xiAuthUrlOpt  = csvFilesForEoris.xiCsvFiles.headOption.map(_.downloadURL)
                val dateOpt       = Formatters.dateAsDayMonthAndYear(
                  Some(csvFilesForEoris.csvFiles.headOption.map(_.startDate).getOrElse(LocalDate.now)).get
                )
                val fileExistsOpt = csvFiles.nonEmpty

                (authUrlOpt, xiAuthUrlOpt, Some(dateOpt), Some(fileExistsOpt))
              } else {
                (None, None, None, None)
              }

            BadRequest(
              authorisedToViewSearch(
                formWithErrors,
                authUrl,
                xiAuthUrl,
                date,
                fileExists,
                appConfig.xiEoriEnabled,
                appConfig.isEUEoriEnabled,
                isAuthoritiesNotificationPanelEnabled
              )
            )
          },
        query => storeQueryAndRedirect(query)
      )
  }

  private def storeQueryAndRedirect(query: String)(implicit request: Request[AnyContent]): Future[Result] = {
    val sessionId = hc.sessionId.getOrElse {
      log.error("Missing SessionID");
      SessionId("Missing Session ID")
    }

    queryRepository
      .clearAndInsertQuery(sessionId.value, query)
      .map { resultWritten =>
        if (resultWritten) {
          Redirect(routes.AuthorizedToViewController.onSearch())
        } else {
          InternalServerError(errorHandler.technicalDifficulties()(request))
        }
      }
      .recoverWith { case _ =>
        Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))
      }
  }

  private def retrieveQueryAndProcess(
    handleQueryFn: String => Future[Result]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    val sessionId = hc.sessionId.getOrElse {
      log.error("Missing SessionID");
      SessionId("Missing Session ID")
    }

    queryRepository
      .getQuery(sessionId.value)
      .flatMap {
        case Some(query) => handleQueryFn(query)
        case _           => Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))
      }
      .recoverWith { case _ =>
        Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))
      }
  }

  def onSearch(): Action[AnyContent] = authenticate async { implicit request =>
    retrieveQueryAndProcess(processSearchQuery(request, _))
  }

  def onNoSearchResult(): Action[AnyContent] = authenticate async { implicit request =>
    retrieveQueryAndProcess(processNoSearchResult)
  }

  private def processNoSearchResult(
    searchQuery: EORI
  )(implicit request: Request[AnyContent], messages: Messages): Future[Result] =
    Future.successful(Ok(authorisedToViewSearchNoResult(searchQuery)(request, messages, appConfig)))

  private def processSearchQuery(request: AuthenticatedRequest[AnyContent], query: EORI)(implicit
    hc: HeaderCarrier,
    messages: Messages,
    appConfig: AppConfig
  ): Future[Result] = {
    val searchQuery = stripWithWhitespace(query)

    val result = for {
      eoriAccounts: CDSAccounts   <- apiService.getAccounts(request.user.eori)
      xiEORI: Option[EORI]        <- dataStoreService.getXiEori
      xiEoriAccounts: CDSAccounts <- getXiEoriCdsAccounts(request, xiEORI)
      csvFiles                    <- getCsvFile()(request)
    } yield {
      val isMyAcc =
        eoriAccounts.myAccounts.exists(_.number == query) || xiEoriAccounts.myAccounts.exists(_.number == query)

      val viewModel                  = csvFiles
      val fileExists                 = csvFiles.nonEmpty
      val csvFilesForEoris: CsvFiles = partitionCsvFilesByFileNamePattern(viewModel)
      val authUrl: Option[EORI]      = csvFilesForEoris.csvFiles.headOption.map(_.downloadURL)
      val xiAuthUrl                  = csvFilesForEoris.xiCsvFiles.headOption.map(_.downloadURL)

      (request.user.eori, isMyAcc, xiEORI) match {
        case (eori, _, _) if eori.equalsIgnoreCase(query) || (xiEORI.isDefined && xiEORI.get.equalsIgnoreCase(query)) =>
          displayErrorView(query, "cf.account.authorized-to-view.search-own-eori", fileExists, authUrl, xiAuthUrl)(
            request,
            messages,
            appConfig
          )

        case (_, true, _) =>
          displayErrorView(
            query,
            "cf.account.authorized-to-view.search-own-accountnumber",
            fileExists,
            authUrl,
            xiAuthUrl
          )(request, messages, appConfig)

        case (_, _, assocXiEori) if assocXiEori.isEmpty && isXIEori(searchQuery) =>
          displayErrorView(query, "cf.search.authorities.error.register-xi-eori", fileExists, authUrl, xiAuthUrl)(
            request,
            messages,
            appConfig
          )

        case _ =>
          if (xiEORI.nonEmpty) {
            searchAuthoritiesForValidInput(request, searchQuery, xiEORI)
          } else {
            searchAuthoritiesForValidInput(request, searchQuery)
          }
      }
    }

    result.flatten
  }

  private def getXiEoriCdsAccounts(request: AuthenticatedRequest[AnyContent], xiEORI: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[CDSAccounts] =
    xiEORI match {
      case Some(x) => apiService.getAccounts(x)
      case None    => Future.successful(CDSAccounts(request.user.eori, None, Seq.empty[CDSAccount]))
    }

  private def displayErrorView(
    query: EORI,
    msgKey: String,
    fileExists: Boolean,
    authUrl: Option[String],
    xiAuthUrl: Option[String]
  )(implicit request: Request[_], messages: Messages, appConfig: AppConfig): Future[Result] =
    Future.successful(
      BadRequest(
        authorisedToViewSearch(
          form.withError("value", msgKey).fill(query),
          authUrl,
          xiAuthUrl,
          Some(LocalDate.now.toString),
          Some(fileExists),
          appConfig.xiEoriEnabled,
          appConfig.isEUEoriEnabled,
          appConfig.isAuthoritiesNotificationPanelEnabled
        )(request, messages, appConfig)
      )
    )

  private def searchAuthoritiesForValidInput(
    request: AuthenticatedRequest[AnyContent],
    searchQuery: EORI,
    xiEORI: Option[String] = None
  )(implicit hc: HeaderCarrier, messages: Messages, appConfig: AppConfig): Future[Result] = {
    val result = for {
      authForEORI   <- apiService.searchAuthorities(request.user.eori, searchQuery)
      authForXIEORI <- if (xiEORI.isDefined) {
                         apiService.searchAuthorities(xiEORI.getOrElse(emptyString), searchQuery)
                       } else {
                         Future.successful(Left(NoAuthorities))
                       }
    } yield (authForEORI, authForXIEORI) match {
      case (Left(NoAuthorities), Left(NoAuthorities)) =>
        Future.successful(Redirect(routes.AuthorizedToViewController.onNoSearchResult()))

      case (Left(SearchError), Left(SearchError)) | (Left(SearchError), Left(NoAuthorities)) |
          (Left(NoAuthorities), Left(SearchError)) =>
        Future.successful(InternalServerError(errorHandler.technicalDifficulties()(request)))

      case (Right(authorities), Left(_)) =>
        processAuthAndViewResultPage(request, searchQuery, messages, appConfig, authorities)

      case (Left(_), Right(xiAuthorities)) =>
        processAuthAndViewResultPage(request, searchQuery, messages, appConfig, xiAuthorities, isNotXIAuth = false)

      case (Right(authorities), Right(xiAuthorities)) =>
        processAuthForAllEorisAndViewResultPage(request, searchQuery, messages, appConfig, authorities, xiAuthorities)
    }

    result.flatten
  }

  private def processAuthAndViewResultPage(
    request: AuthenticatedRequest[AnyContent],
    searchQuery: EORI,
    messages: Messages,
    appConfig: AppConfig,
    searchedAuthorities: SearchedAuthorities,
    isNotXIAuth: Boolean = true
  )(implicit hc: HeaderCarrier): Future[Result] = {

    val displayLink: Boolean = getDisplayLink(searchedAuthorities)
    val clientEori: EORI     = getClientEori(searchedAuthorities)

    dataStoreService.getCompanyName(clientEori).flatMap { companyName =>

      val searchResultView = if (isNotXIAuth) {
        authorisedToViewSearchResult(searchQuery, Option(clientEori), searchedAuthorities, companyName, displayLink)(
          request,
          messages,
          appConfig
        )
      } else {
        authorisedToViewSearchResult(
          searchQuery,
          None,
          searchedAuthorities,
          companyName,
          displayLink,
          Option(clientEori)
        )(request, messages, appConfig)
      }

      Future.successful(Ok(searchResultView))
    }
  }

  private def processAuthForAllEorisAndViewResultPage(
    request: AuthenticatedRequest[AnyContent],
    searchQuery: EORI,
    messages: Messages,
    appConfig: AppConfig,
    authorities: SearchedAuthorities,
    xiAuthorities: SearchedAuthorities
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val displayLinkForAuth   = getDisplayLink(authorities)
    val displayLinkForXIAuth = getDisplayLink(xiAuthorities)
    val displayLink          = displayLinkForAuth && displayLinkForXIAuth

    val eori: EORI   = getClientEori(authorities)
    val xiEori: EORI = getClientEori(xiAuthorities)

    dataStoreService.getCompanyName(eori).flatMap { companyName =>
      Future.successful(
        Ok(
          authorisedToViewSearchResult(
            searchQuery,
            Option(eori),
            finalSearchAuthoritiesToShow(authorities, xiAuthorities),
            companyName,
            displayLink,
            Option(xiEori)
          )(request, messages, appConfig)
        )
      )
    }
  }

  private def finalSearchAuthoritiesToShow(
    authorities: SearchedAuthorities,
    xiAuthorities: SearchedAuthorities
  ): SearchedAuthorities = {
    val listOfEligibleAuthorities = List(authorities, xiAuthorities).filter(sAuth => !getDisplayLink(sAuth))

    if (listOfEligibleAuthorities.isEmpty) authorities else listOfEligibleAuthorities.head
  }

  private def getCsvFile()(implicit req: AuthenticatedRequest[_]): Future[Seq[StandingAuthorityFile]] =
    sdesConnector
      .getAuthoritiesCsvFiles(req.user.eori)
      .map(_.sortWith(_.startDate isAfter _.startDate).sortBy(_.filename).toSeq.sortWith(_.filename > _.filename))

  private def getClientEori(searchedAuthorities: SearchedAuthorities) =
    searchedAuthorities.authorities.map {
      case AuthorisedDutyDefermentAccount(account, _)    => account.accountOwner
      case AuthorisedCashAccount(account, _)             => account.accountOwner
      case AuthorisedGeneralGuaranteeAccount(account, _) => account.accountOwner
    }.head

  private def getDisplayLink(searchedAuthorities: SearchedAuthorities): Boolean =
    searchedAuthorities.authorities.exists {
      case AuthorisedDutyDefermentAccount(_, balances)                     => balances.map(_.periodAvailableAccountBalance).isEmpty
      case AuthorisedCashAccount(_, availableAccountBalance)               => availableAccountBalance.isEmpty
      case AuthorisedGeneralGuaranteeAccount(_, availableGuaranteeBalance) => availableGuaranteeBalance.isEmpty
    }

  private def stripWithWhitespace(str: String): String = str.replaceAll("\\s", emptyString).toUpperCase
}

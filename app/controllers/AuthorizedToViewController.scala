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
import domain.FileRole.{StandingAuthority}
import domain._
import forms.EoriNumberFormProvider
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.helpers.Formatters
import views.html.authorised_to_view._

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

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
        csvFiles <- getCsvFile(req.user.eori)
      } yield {
        val viewmodel = csvFiles
        val fileExists = csvFiles.nonEmpty
        val url = Some(viewmodel.headOption.map(_.downloadURL).getOrElse(""))
        val date = Formatters.dateAsDayMonthAndYear(Some(viewmodel.headOption.map(_.startDate).getOrElse(LocalDate.now)).get)
        Ok(authorisedToViewSearch(form, url, date, fileExists))
      }
  }

  private def getCsvFile(eori: String)(implicit req: AuthenticatedRequest[_]): Future[Seq[StandingAuthorityFile]] = {
    sdesConnector.getAuthoritiesCsvFiles(eori)
      .map(_.sortWith(_.startDate isAfter _.startDate).sortBy(_.filename).toSeq.sortWith(_.filename > _.filename))
  }

  def onSubmit(): Action[AnyContent] = authenticate async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        for {
          csvFiles <- getCsvFile(request.user.eori)
        } yield {
          val viewmodel = csvFiles
          val fileExists = csvFiles.nonEmpty
          val url = Some(viewmodel.headOption.map(_.downloadURL).getOrElse(""))
          val date = Formatters.dateAsDayMonthAndYear(Some(viewmodel.headOption.map(_.startDate).getOrElse(LocalDate.now)).get)
          BadRequest(authorisedToViewSearch(formWithErrors, url, date, fileExists))
        },
      query => {
        val searchQuery = stripWithWhitespace(query)

        val maxWaitTime: FiniteDuration = Duration(5, TimeUnit.SECONDS)
        val isMyAcc = Await.result(apiService.getAccounts(request.user.eori).map(
          _.myAccounts.exists(_.number == query)), maxWaitTime)

        if (request.user.eori.equalsIgnoreCase(query)) {
          Future.successful(BadRequest(authorisedToViewSearch(form.withError("value",
            "cf.account.authorized-to-view.search-own-eori").fill(query),
              Some(""), LocalDate.now.toString, false)))
         }  else if (isMyAcc) {
            Future.successful(BadRequest(authorisedToViewSearch(form.withError("value",
              "cf.account.authorized-to-view.search-own-accountnumber").fill(query),
                Some(""), LocalDate.now.toString, false)))
          }
        else {
          apiService.searchAuthorities(request.user.eori, searchQuery).flatMap {
            case Left(NoAuthorities) => Future.successful(Ok(authorisedToViewSearchNoResult(searchQuery)))
            case Left(SearchError) => Future.successful(InternalServerError(errorHandler.technicalDifficulties))
            case Right(searchedAuthorities) => {

              val displayLink: Boolean = getDisplayLink(searchedAuthorities)
              val clientEori: String = getClientEori(searchedAuthorities)

              dataStoreService.getCompanyName(clientEori).map { companyName => {
                Ok(authorisedToViewSearchResult(searchQuery, clientEori, searchedAuthorities, companyName, displayLink))}
              }
            }
          }
        }
      }
    )
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
    str.replaceAll("\\s", "")
}

/*
 * Copyright 2022 HM Revenue & Customs
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

import actionbuilders.IdentifierAction
import config.{AppConfig, ErrorHandler}
import connectors.CustomsFinancialsApiConnector
import domain.FileRole.StandingAuthority
import domain.{AuthorisedCashAccount, AuthorisedDutyDefermentAccount, AuthorisedGeneralGuaranteeAccount, AuthorizedToViewPageState, NoAuthorities, SearchError}
import forms.EoriNumberFormProvider
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.authorised_to_view.{authorised_to_view_search, authorised_to_view_search_no_result, authorised_to_view_search_result, authorized_to_view}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorizedToViewController @Inject()(authenticate: IdentifierAction,
                                           apiService: ApiService,
                                           errorHandler: ErrorHandler,
                                           dataStoreService: DataStoreService,
                                           financialsApiConnector: CustomsFinancialsApiConnector,
                                           implicit val mcc: MessagesControllerComponents,
                                           authorizedView: authorized_to_view,
                                           authorisedToViewSearch: authorised_to_view_search,
                                           authorisedToViewSearchResult: authorised_to_view_search_result,
                                           authorisedToViewSearchNoResult: authorised_to_view_search_no_result,
                                           eoriNumberFormProvider: EoriNumberFormProvider)(implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)
  val form: Form[String] = eoriNumberFormProvider()

  def onPageLoad(pageState: AuthorizedToViewPageState): Action[AnyContent] = authenticate async { implicit req =>
    financialsApiConnector.deleteNotification(req.user.eori, StandingAuthority)

    if (!appConfig.newAgentView) {
      val eori = req.user.eori
      val result = for {
        accounts <- apiService.getAccounts(eori).map(_.authorizedToView)
      } yield {
        val viewModel = viewmodels.AuthorizedToViewViewModel(req.user.eori, accounts, pageState)
        Ok(authorizedView(viewModel))
      }
      result.recover {
        case _: GatewayTimeoutException =>
          log.warn(s"Request Timeout while fetching accounts")
          Redirect(routes.CustomsFinancialsHomeController.showAccountUnavailable)
      }
    } else {
      Future.successful(Ok(authorisedToViewSearch(form)))
    }
  }

  def onSubmit(): Action[AnyContent] = authenticate async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(authorisedToViewSearch(formWithErrors))),
      query =>
        apiService.searchAuthorities(request.user.eori, query).flatMap {
          case Left(NoAuthorities) => Future.successful(Ok(authorisedToViewSearchNoResult(query)))
          case Left(SearchError) => Future.successful(InternalServerError(errorHandler.technicalDifficulties))
          case Right(searchedAuthorities) =>
            val clientEori = searchedAuthorities.authorities.map{
              case AuthorisedDutyDefermentAccount(account, balances) => account.accountOwner
              case AuthorisedCashAccount(account, availableAccountBalance) => account.accountOwner
              case AuthorisedGeneralGuaranteeAccount(account, availableGuaranteeBalance) => account.accountOwner
            }.head
            dataStoreService.getCompanyName(clientEori).map { companyName => {
              Ok(authorisedToViewSearchResult(query, clientEori, searchedAuthorities, companyName))
            }
            }
        }
    )
  }
}


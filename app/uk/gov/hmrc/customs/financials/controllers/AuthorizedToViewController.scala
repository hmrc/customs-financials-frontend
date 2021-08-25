/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.actionbuilders.IdentifierAction
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.AuthorizedToViewPageState
import uk.gov.hmrc.customs.financials.services.ApiService
import uk.gov.hmrc.customs.financials.viewmodels.AuthorizedToViewViewModel
import uk.gov.hmrc.customs.financials.views.html.authorized_to_view
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuthorizedToViewController @Inject()(authenticate: IdentifierAction,
                                           apiService: ApiService,
                                           implicit val mcc: MessagesControllerComponents,
                                           authorizedView: authorized_to_view)(implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def onPageLoad(pageState: AuthorizedToViewPageState): Action[AnyContent] = authenticate async { implicit req =>
    val eori = req.user.eori
    val result = for {
      accounts <- apiService.getAccounts(eori).map(_.authorizedToView)
    } yield {
      val viewModel = AuthorizedToViewViewModel(req.user.eori, accounts, pageState)
      Ok(authorizedView(viewModel))
    }
    result.recover {
      case _: GatewayTimeoutException =>
        log.warn(s"Request Timeout while fetching accounts")
        Redirect(routes.CustomsFinancialsHomeController.showAccountUnavailable())
    }
  }
}


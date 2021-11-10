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

package controllers

import actionbuilders.IdentifierAction
import config.AppConfig
import domain.AuthorizedToViewPageState
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import services.ApiService
import uk.gov.hmrc.http.GatewayTimeoutException
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.authorised_to_view.authorized_to_view

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
      val viewModel = viewmodels.AuthorizedToViewViewModel(req.user.eori, accounts, pageState)
      Ok(authorizedView(viewModel))
    }
    result.recover {
      case _: GatewayTimeoutException =>
        log.warn(s"Request Timeout while fetching accounts")
        Redirect(routes.CustomsFinancialsHomeController.showAccountUnavailable())
    }
  }
}


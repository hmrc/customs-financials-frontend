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

import actionbuilders.IdentifierAction
import config.AppConfig
import connectors.CustomsDataStoreConnector
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.email.{undeliverable_email, verify_your_email}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EmailController @Inject() (
  authenticate: IdentifierAction,
  verifyEmailView: verify_your_email,
  undeliverableEmail: undeliverable_email,
  customsDataStoreConnector: CustomsDataStoreConnector,
  implicit val mcc: MessagesControllerComponents
)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def showUnverified(): Action[AnyContent] = authenticate async { implicit request =>
    customsDataStoreConnector.isEmailUnverified.map(email => Ok(verifyEmailView(appConfig.emailFrontendUrl, email)))
  }

  def showUndeliverable(): Action[AnyContent] = authenticate async { implicit request =>
    customsDataStoreConnector.getEmailAddress.map(verifiedEmailRes =>
      Ok(
        undeliverableEmail(appConfig.emailFrontendUrl, verifiedEmailRes.verifiedEmail)(
          request,
          request.messages,
          appConfig
        )
      )
    )
  }
}

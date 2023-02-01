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
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DataStoreService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.your_contact_details.your_contact_details
import scala.concurrent.{ExecutionContext, Future}

class YourContactDetailsController @Inject()(authenticate: IdentifierAction,
                                       override val messagesApi: MessagesApi,
                                       dataStoreService: DataStoreService,
                                       view: your_contact_details,
                                       implicit val mcc: MessagesControllerComponents)
                                       (implicit val appConfig: AppConfig, ec: ExecutionContext)
                                       extends FrontendController(mcc) with I18nSupport {

  def onPageLoad(): Action[AnyContent] = authenticate async { implicit request =>

    for {
      companyName <- dataStoreService.getCompanyName(request.user.eori)
      email <- dataStoreService.getEmail(request.user.eori).flatMap {
        case Left(_) => Future.successful(InternalServerError)
        case Right(email) => Future.successful(email.value)
      }

    } yield {
      Ok(view(request.user.eori,"AccountNumber",companyName,"Address", email.toString))
    }
  }
}

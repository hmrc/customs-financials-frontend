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
import forms.EoriNumberFormProvider
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.authorised_to_view.authorised_to_view_request_received
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorizedRequestReceivedController @Inject()(authenticate: IdentifierAction,
                                                    apiService: ApiService,
                                                    errorHandler: ErrorHandler,
                                                    customsDataStore: DataStoreService,
                                                    implicit val mcc: MessagesControllerComponents,
                                                    authorisedToViewRequestReceived: authorised_to_view_request_received,
                                                    eoriNumberFormProvider: EoriNumberFormProvider)(implicit val appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)
  val form: Form[String] = eoriNumberFormProvider()

  def requestAuthoritiesCsv(): Action[AnyContent] = authenticate async { implicit req =>
    customsDataStore.getEmail(req.user.eori).flatMap {
      case Right(email) =>
        apiService.requestAuthoritiesCsv(req.user.eori).flatMap {
        case Right(_) =>
          Future.successful(Ok(authorisedToViewRequestReceived(email.value)))
        case _ =>
          Future.successful(InternalServerError(errorHandler.technicalDifficulties))
      }
      case _ => Future.successful(InternalServerError(errorHandler.technicalDifficulties))
    }
  }
}

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
import config.AppConfig
import connectors.CustomsFinancialsSessionCacheConnector
import domain.{AccountLinkWithoutDate, CompanyAddress}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.{Logger, LoggerLike}
import services.DataStoreService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.your_contact_details.your_contact_details
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourContactDetailsController @Inject()(authenticate: IdentifierAction,
                                       override val messagesApi: MessagesApi,
                                       dataStoreService: DataStoreService,
                                       view: your_contact_details,
                                       sessionCacheConnector: CustomsFinancialsSessionCacheConnector,
                                       implicit val mcc: MessagesControllerComponents)
                                       (implicit val appConfig: AppConfig, ec: ExecutionContext)
                                       extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def onPageLoad(): Action[AnyContent] = authenticate async { implicit request =>

    val localSessionId: SessionId = getSessionId()

    localSessionId match {
      case sessionCache => sessionCacheConnector.getSessionId(localSessionId.value) match {
        case _ if sessionCache.value == localSessionId.value => generateView(request,localSessionId)
        case _ => redirectToHomePage()
      }
    }
  }

  private def generateView(request: AuthenticatedRequest[AnyContent],
                           localSessionId: SessionId)(implicit hc: HeaderCarrier,
                           messages: Messages,
                           appConfig: AppConfig): Future[Result] = {
    for {
      email <- dataStoreService.getEmail(request.user.eori).flatMap {
        case Right(email) => Future.successful(email.value)
        case Left(_) => Future.successful(InternalServerError)
      }

      companyName <- dataStoreService.getOwnCompanyName(request.user.eori)
      dataStoreAddress <- dataStoreService.getCompanyAddress(request.user.eori)
      companyAddress: CompanyAddress = dataStoreAddress.getOrElse(
        new CompanyAddress("", "", Some(""), ""))

      address = CompanyAddress(
        streetAndNumber = companyAddress.streetAndNumber,
        city = companyAddress.city,
        postalCode = companyAddress.postalCode,
        countryCode = companyAddress.countryCode)

      accountlinks <- sessionCacheConnector.getAccontLinks(localSessionId.value)
    } yield {
      Ok(view(request.user.eori, accountlinks.getOrElse(Seq.empty[AccountLinkWithoutDate]),
        companyName, address, email.toString)(request, messages, appConfig))
    }
  }

  private def redirectToHomePage(): Future[Result] = {
    Future.successful(Redirect(routes.CustomsFinancialsHomeController.index.url))
  }

  private def getSessionId()(implicit hc: HeaderCarrier): SessionId = {
    SessionId(hc.sessionId.getOrElse(
      {log.error("Missing SessionID"); SessionId("Missing Session ID")}).value)
  }
}

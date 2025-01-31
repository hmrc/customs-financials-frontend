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
import connectors.{CustomsFinancialsSessionCacheConnector, SecureMessageConnector}
import domain.{AccountLinkWithoutDate, CompanyAddress}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader, Result}
import play.api.{Logger, LoggerLike}
import services.DataStoreService
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Utils.emptyString
import views.html.your_contact_details.your_contact_details

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourContactDetailsController @Inject() (
  authenticate: IdentifierAction,
  override val messagesApi: MessagesApi,
  dataStoreService: DataStoreService,
  view: your_contact_details,
  sessionCacheConnector: CustomsFinancialsSessionCacheConnector,
  secureMessageConnector: SecureMessageConnector
)(implicit val appConfig: AppConfig, ec: ExecutionContext, mcc: MessagesControllerComponents)
    extends FrontendController(mcc)
    with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def onPageLoad(): Action[AnyContent] = authenticate async { implicit request =>
    hc.sessionId match {
      case Some(headerId) => verifySessionAndViewPage(request, headerId)
      case _              =>
        log.error("Missing SessionID")
        redirectToHomePage()
    }
  }

  private def verifySessionAndViewPage(request: AuthenticatedRequest[AnyContent], headerId: SessionId)(implicit
    hc: HeaderCarrier,
    messages: Messages,
    appConfig: AppConfig,
    requestHeader: RequestHeader
  ): Future[Result] =
    sessionCacheConnector.getSessionId(headerId.value).flatMap {
      case Some(cacheId) =>
        if (cacheId.body == headerId.value) {
          generateView(request, headerId)
        } else {
          redirectToHomePage()
        }

      case _ =>
        log.error("Missing SessionID")
        redirectToHomePage()
    }

  private def generateView(request: AuthenticatedRequest[AnyContent], localSessionId: SessionId)(implicit
    hc: HeaderCarrier,
    messages: Messages,
    appConfig: AppConfig,
    requestHeader: RequestHeader
  ): Future[Result] = {
    val returnToUrl =
      s"${appConfig.financialsFrontendUrl}${controllers.routes.YourContactDetailsController.onPageLoad()}"

    for {
      email <- dataStoreService.getEmail.flatMap {
                 case Right(email) => Future.successful(email.value)
                 case Left(_)      => Future.successful(InternalServerError)
               }

      companyName      <- dataStoreService.getOwnCompanyName
      dataStoreAddress <- dataStoreService.getCompanyAddress
      companyAddress    =
        dataStoreAddress.getOrElse(CompanyAddress(emptyString, emptyString, Some(emptyString), emptyString))

      address = CompanyAddress(
                  streetAndNumber = companyAddress.streetAndNumber,
                  city = companyAddress.city,
                  postalCode = companyAddress.postalCode,
                  countryCode = companyAddress.countryCode
                )

      accountLinks         <- sessionCacheConnector.getAccontLinks(localSessionId.value)
      messageBannerPartial <- secureMessageConnector.getMessageCountBanner(returnToUrl)
    } yield Ok(
      view(
        request.user.eori,
        accountLinks.getOrElse(Seq.empty[AccountLinkWithoutDate]),
        companyName,
        address,
        email.toString,
        messageBannerPartial.map(_.successfulContentOrEmpty)
      )(request, messages, appConfig)
    )
  }

  private def redirectToHomePage(): Future[Result] =
    Future.successful(Redirect(routes.CustomsFinancialsHomeController.index.url))
}

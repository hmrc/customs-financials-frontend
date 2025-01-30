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
import config.AppConfig
import connectors.{CustomsFinancialsSessionCacheConnector, CustomsManageAuthoritiesConnector, SecureMessageConnector}
import domain.FileRole.{DutyDefermentStatement, PostponedVATAmendedStatement, StandingAuthority}
import domain._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.{Logger, LoggerLike}
import services._
import uk.gov.hmrc.http.{GatewayTimeoutException, SessionId}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.partials.HtmlPartial
import viewmodels.FinancialsHomeModel
import views.html.dashboard.{customs_financials_home, customs_financials_partial_home}
import views.html.error_states.account_not_available
import utils.Utils.{emptyString, hyphen}

import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CustomsFinancialsHomeController @Inject() (
  authenticate: IdentifierAction,
  checkEmailIsVerified: EmailAction,
  apiService: ApiService,
  auditingService: AuditingService,
  dataStoreService: DataStoreService,
  notificationService: NotificationService,
  customsHomeView: customs_financials_home,
  customsHomePartialView: customs_financials_partial_home,
  accountNotAvailable: account_not_available,
  sessionCacheConnector: CustomsFinancialsSessionCacheConnector,
  secureMessageConnector: SecureMessageConnector,
  customsManageAuthConnector: CustomsManageAuthoritiesConnector,
  implicit val mcc: MessagesControllerComponents
)(implicit val appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def index: Action[AnyContent] = (authenticate andThen checkEmailIsVerified).async { implicit request =>

    val returnToUrl = appConfig.financialsFrontendUrl + controllers.routes.CustomsFinancialsHomeController.index.url
    val eori        = request.user.eori

    customsManageAuthConnector.fetchAndSaveAccountAuthoritiesInCache(eori)

    val result = for {
      _                  <- auditingService.viewAccount(request.user)
      maybeBannerPartial <- secureMessageConnector.getMessageCountBanner(returnToUrl)
      xiEori             <- dataStoreService.getXiEori()
      allAccounts        <- getAllAccounts(eori, xiEori)
      page               <- if (allAccounts.nonEmpty) {
                              pageWithAccounts(eori, xiEori, allAccounts, maybeBannerPartial)
                            } else {
                              redirectToPageWithoutAccounts()
                            }
    } yield page
    result.recover { case TimeoutResponse =>
      Redirect(controllers.routes.CustomsFinancialsHomeController.showAccountUnavailable)
    }
  }

  private def getAllAccounts(eori: EORI, xiEori: Option[String])(implicit
    request: AuthenticatedRequest[AnyContent]
  ): Future[Seq[CDSAccounts]] = {
    val eoriList         = Seq(eori, xiEori.getOrElse(emptyString)).filterNot(_ == emptyString)
    val seqOfEoriHistory = request.user.allEoriHistory.filterNot(_.eori == eori)

    for {
      accounts         <- Future.sequence(eoriList.map(eachEori => apiService.getAccounts(eachEori)))
      historicAccounts <- Future.sequence(seqOfEoriHistory.map(each => apiService.getAccounts(each.eori)))
    } yield historicAccounts ++ accounts

  } recoverWith {
    case _: GatewayTimeoutException =>
      log.warn(s"Request Timeout while fetching accounts")
      Future.failed(TimeoutResponse)

    case NonFatal(e) =>
      log.warn(s"[GetAccounts API] Failed with error: ${e.getMessage}")
      Future.successful(Seq.empty[CDSAccounts])
  }

  private def redirectToPageWithoutAccounts(): Future[Result] =
    Future.successful(Redirect(routes.CustomsFinancialsHomeController.pageWithoutAccounts))

  private def pageWithAccounts(
    eori: EORI,
    xiEori: Option[String],
    cdsAccountsList: Seq[CDSAccounts],
    maybeBannerPartial: Option[HtmlPartial]
  )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] =
    for {
      notificationMessageKeys <- notificationService.fetchNotifications(eori).map(getNotificationMessageKeys)
      companyName             <- dataStoreService.getOwnCompanyName()
      sessionId                = hc.sessionId.getOrElse {
                                   log.error("Missing SessionID");
                                   SessionId("Missing Session ID")
                                 }
      accountLinks             = createAccountLinks(sessionId, cdsAccountsList)
      _                       <- sessionCacheConnector.storeSession(sessionId.value, accountLinks)
    } yield {
      val model = FinancialsHomeModel(eori, companyName, cdsAccountsList, notificationMessageKeys, accountLinks, xiEori)
      Ok(customsHomeView(model, maybeBannerPartial.map(_.successfulContentOrEmpty)))
    }

  def createAccountLinks(sessionId: SessionId, cdsAccountsList: Seq[CDSAccounts]): Seq[AccountLink] = for {
    cdsAccounts <- cdsAccountsList
    cdsAccount  <- cdsAccounts.accounts
    accountLink  = AccountLink(
                     sessionId.value,
                     cdsAccount.owner,
                     cdsAccounts.isNiAccount.getOrElse(false),
                     cdsAccount.number,
                     cdsAccount.status,
                     Option(cdsAccount.statusId),
                     UUID.randomUUID().toString.replaceAll(hyphen, emptyString),
                     LocalDateTime.now()
                   )
  } yield accountLink

  def pageWithoutAccounts: Action[AnyContent] = authenticate.async { implicit request =>
    val eori = request.user.eori

    notificationService
      .fetchNotifications(eori)
      .map(_.filterNot(v => (v.fileRole == DutyDefermentStatement) || (v.fileRole == StandingAuthority)))
      .map(getNotificationMessageKeys)
      .map(keys => Ok(customsHomePartialView(eori, keys)))
  }

  def getNotificationMessageKeys(collectionOfDocumentAttributes: Seq[Notification]): Seq[String] = {

    val requestedNotifications: Seq[Notification] =
      collectionOfDocumentAttributes.filter(v => v.isRequested && v.fileRole != PostponedVATAmendedStatement).distinct

    val statementNotifications: Seq[Notification] =
      collectionOfDocumentAttributes.filterNot(v => v.isRequested || v.fileRole == StandingAuthority)

    val authoritiesNotification: Seq[Notification] =
      collectionOfDocumentAttributes.filter(_.fileRole == StandingAuthority).distinct

    val requestedMessages = requestedNotifications.map(notification => s"requested-${notification.fileRole.messageKey}")

    val statementMessages = statementNotifications.groupBy(_.fileRole).toSeq.map {
      case (role, notifications) if notifications.size > 1 => s"multiple-${role.messageKey}"
      case (role, _)                                       => role.messageKey
    }

    val authorityMessage = authoritiesNotification.map(notification => s"${notification.fileRole.messageKey}")

    authorityMessage ++ requestedMessages ++ statementMessages
  }

  def showAccountUnavailable: Action[AnyContent] = authenticate.async { implicit req =>
    val eori = req.user.eori

    notificationService
      .fetchNotifications(eori)
      .map(_.filterNot(_.fileRole == DutyDefermentStatement))
      .map(getNotificationMessageKeys)
      .map(keys => Ok(accountNotAvailable(eori, keys)))
  }
}

case object TimeoutResponse extends Exception

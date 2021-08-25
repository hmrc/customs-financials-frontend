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

import cats.data.EitherT.{fromOption, fromOptionF, liftF}
import cats.instances.future._
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.actionbuilders.{IdentifierAction, SessionIdAction}
import uk.gov.hmrc.customs.financials.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.domain.AccountLink
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DutyDefermentContactDetailsController @Inject()(authenticate: IdentifierAction,
                                                      resolveSessionId: SessionIdAction,
                                                      contactDetailsService: ContactDetailsService,
                                                      sessionCacheConnector:CustomsFinancialsSessionCacheConnector,
                                                      implicit val mcc: MessagesControllerComponents)
                                                     (implicit val appConfig: AppConfig,
                                                      errorHandler: ErrorHandler,
                                                      ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def showContactDetails(linkId: String): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>
    val link = linkId.split('+').head
    (for {
      accountLink <- fromOptionF(getAccountLink(req.sessionId.value, link), NotFound(errorHandler.notFoundTemplate(req)))
      accountStatusId <- fromOption(accountLink.accountStatusId, NotFound(errorHandler.notFoundTemplate(req)))
      contactDetailsUrl <- liftF[Future, Result, String](contactDetailsService.getEncyptedDanWithStatus(accountLink.accountNumber,accountStatusId.value))
    } yield Redirect(contactDetailsUrl)).merge
      .recover { case _ => internalServerErrorFromContactDetails }
  }

  def internalServerErrorFromContactDetails(implicit request: Request[_]): Result = {
    log.error("InternalServerError from Contact Details")
    Ok(errorHandler.contactDetailsErrorTemplate())
  }

  private def getAccountLink(sessionId: String, linkId: String)(implicit hc:HeaderCarrier): Future[Option[AccountLink]] ={
    sessionCacheConnector.retrieveSession(sessionId, linkId)
  }
}

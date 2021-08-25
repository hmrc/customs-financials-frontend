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

import cats.data.EitherT._
import cats.instances.future._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.actionbuilders.{AuthenticatedRequestWithSessionId, IdentifierAction, SessionIdAction}
import uk.gov.hmrc.customs.financials.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.domain.FileRole._
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.customs.financials.viewmodels.{DutyDefermentAccount, DutyDefermentStatementsForEori}
import uk.gov.hmrc.customs.financials.views.html.{duty_deferment_account, duty_deferment_statements_not_available}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class DutyDefermentAccountController @Inject()(val authenticate: IdentifierAction,
                                               val resolveSessionId: SessionIdAction,
                                               val documentService: DocumentService,
                                               apiService: ApiService,
                                               dutyDefermentAccountView: duty_deferment_account,
                                               dutyDefermentStatementsNotAvailableView: duty_deferment_statements_not_available,
                                               sessionCacheConnector: CustomsFinancialsSessionCacheConnector,
                                               implicit val mcc: MessagesControllerComponents)
                                              (implicit val appConfig: AppConfig, val errorHandler: ErrorHandler, ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def showAccountDetails(linkId: String): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>
    apiService.deleteNotification(req.user.eori, DutyDefermentStatement)

    val eventualMaybeAccountLink = getAccountLink(req.sessionId.value, linkId)
    (for {
      accountLink <- fromOptionF(eventualMaybeAccountLink, Redirect(routes.CustomsFinancialsHomeController.index().url))
      historicEoris = req.user.allEoriHistory
      statementsForEoris <- liftF[Future, Result, Seq[DutyDefermentStatementsForEori]](Future.sequence(
        historicEoris.map(historicEori => statementsFromEoriHistory(historicEori, accountLink))
      ))
    } yield {
      val dutyDefermentViewModel = DutyDefermentAccount(accountLink.accountNumber, statementsForEoris, accountLink.linkId)
      Ok(dutyDefermentAccountView(dutyDefermentViewModel))
    }
      ).merge.recover {
      case NonFatal(e) => {
        log.error(s"Unable to retrieve Duty deferment statements :${e.getMessage}")
        Redirect(routes.DutyDefermentAccountController.statementsUnavailablePage(linkId))
      }
    }
  }

  def statementsUnavailablePage(linkId: String): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>
    val eventualMaybeAccountLink = getAccountLink(req.sessionId.value, linkId)

    eventualMaybeAccountLink.map {
      accountLink => accountLink.fold(Unauthorized(errorHandler.unauthorized()))(link => Ok(dutyDefermentStatementsNotAvailableView(link.accountNumber, linkId)))
    }
  }

  private def getAccountLink(sessionId: String, linkId: String)(implicit hc: HeaderCarrier): Future[Option[AccountLink]] = {
    sessionCacheConnector.retrieveSession(sessionId, linkId)
  }

  private def statementsFromEoriHistory(eoriHistory: EoriHistory, accountLink: AccountLink)(implicit req: AuthenticatedRequestWithSessionId[_]): Future[DutyDefermentStatementsForEori] = {
    documentService.getDutyDefermentStatements(eoriHistory.eori, accountLink.accountNumber)
      .map(_.partition(_.metadata.statementRequestId.isEmpty))
      .map {
        case (current, requested) => DutyDefermentStatementsForEori(eoriHistory, current, requested)
      }
  }
}






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
import uk.gov.hmrc.customs.financials.actionbuilders.{AuthenticatedRequestWithSessionId, IdentifierAction, SessionIdAction}
import uk.gov.hmrc.customs.financials.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.customs.financials.domain.FileRole.SecurityStatement
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.customs.financials.viewmodels.SecurityStatementsViewModel
import uk.gov.hmrc.customs.financials.views.html.{security_statements, security_statements_not_available}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SecuritiesController @Inject()(val authenticate: IdentifierAction,
                                     val resolveSessionId: SessionIdAction,
                                     val documentService: DocumentService,
                                     apiService: ApiService,
                                     securityStatementsView: security_statements,
                                     securityStatementsNotAvailableView: security_statements_not_available,
                                     implicit val mcc: MessagesControllerComponents
                                    )(implicit val appConfig: AppConfig, val errorHandler: ErrorHandler, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def showSecurityStatements(): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>
    apiService.deleteNotification(req.user.eori, SecurityStatement)

    (for {
      allStatements <- Future.sequence(req.user.allEoriHistory.map(getStatements))
      securityStatementsModel = SecurityStatementsViewModel(allStatements.sorted)
    } yield Ok(securityStatementsView(securityStatementsModel))
      ).recover {
      case e =>
        log.error(s"Unable to retrieve securities statements :${e.getMessage}")
        Redirect(routes.SecuritiesController.statementsUnavailablePage())
    }
  }

  def statementsUnavailablePage(): Action[AnyContent] = authenticate async { implicit req =>
    Future.successful(Ok(securityStatementsNotAvailableView()))
  }

  private def getStatements(historicEori: EoriHistory)(implicit req: AuthenticatedRequestWithSessionId[_]): Future[SecurityStatementsForEori] = {
    documentService.getSecurityStatements(historicEori.eori)
      .map(groupByMonthDescending)
      .map(_.partition(_.files.exists(_.metadata.statementRequestId.isEmpty)))
      .map {
        case (current, requested) => SecurityStatementsForEori(historicEori, current, requested)
      }
  }

  private def groupByMonthDescending(securityStatementFiles: Seq[SecurityStatementFile]): Seq[SecurityStatementsByPeriod] = {
    securityStatementFiles.groupBy(file => (file.startDate, file.endDate)).map {
      case ((startDate, endDate), filesForMonth) => SecurityStatementsByPeriod(startDate, endDate, filesForMonth)
    }.toList.sorted.reverse
  }

}






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

import cats.data.EitherT
import cats.data.EitherT.{fromOptionF, liftF}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.actionbuilders.{IdentifierAction, SessionIdAction}
import uk.gov.hmrc.customs.financials.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DutyDefermentDirectDebitSetupController @Inject()(authenticate: IdentifierAction,
                                                        resolveSessionId: SessionIdAction,
                                                        directDebitService: DirectDebitService,
                                                        dataStoreService: DataStoreService,
                                                        sessionCacheConnector: CustomsFinancialsSessionCacheConnector,
                                                        implicit val mcc: MessagesControllerComponents)
                                                       (implicit val appConfig: AppConfig,
                                                        errorHandler: ErrorHandler,
                                                        ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def setup(linkId: String): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>
    val returnUrl: String = s"${appConfig.customsFinancialFrontend}${routes.CustomsFinancialsHomeController.index().url}"
    val backUrl: String = s"${appConfig.customsFinancialFrontend}${routes.CustomsFinancialsHomeController.index().url}"
    val result: EitherT[Future, Result, Result] = for {
      accountLink <- fromOptionF(
        sessionCacheConnector.retrieveSession(req.sessionId.value, linkId),
        NotFound(errorHandler.notFoundTemplate(req))
      )
      email <- fromOptionF(
        dataStoreService.getEmail(req.user.eori).map(_.toOption.map(_.value)),
        InternalServerError(errorHandler.sddsErrorTemplate())
      )
      directDebitSetupUrl <- liftF(directDebitService.getDirectDebitSetupURL(
        returnUrl,
        backUrl,
        accountLink.accountNumber,
        email)
      )
    } yield Redirect(directDebitSetupUrl)

    result.merge.recover {
      case _ =>
        log.error("InternalServerError from SDDS")
        InternalServerError(errorHandler.sddsErrorTemplate())
    }
  }
}

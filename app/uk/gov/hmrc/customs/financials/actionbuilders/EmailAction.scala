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

package uk.gov.hmrc.customs.financials.actionbuilders

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.{UndeliverableEmail, UnverifiedEmail}
import uk.gov.hmrc.customs.financials.services.DataStoreService
import uk.gov.hmrc.customs.financials.views.html.undeliverable_email
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailAction @Inject()(dataStoreService: DataStoreService,
                            undeliverableEmail: undeliverable_email,
                            appConfig: AppConfig)(implicit val executionContext: ExecutionContext, val messagesApi: MessagesApi) extends ActionFilter[AuthenticatedRequest] with I18nSupport {
  def filter[A](request: AuthenticatedRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    dataStoreService.getEmail(request.user.eori).map {
      case Left(value) =>
        value match {
          case UndeliverableEmail(email) => Some(Ok(undeliverableEmail(appConfig.customsEmailFrontend, email)(request, request.messages , appConfig)))
          case UnverifiedEmail => Some(Redirect(uk.gov.hmrc.customs.financials.controllers.routes.EmailController.showUnverified()))
        }
      case Right(_) => None
    }.recover { case _ => None } //This will allow users to access the service if ETMP return an error via SUB09
  }
}


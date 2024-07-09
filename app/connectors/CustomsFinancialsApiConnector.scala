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

package connectors

import config.AppConfig
import domain.{EmailUnverifiedResponse, EmailVerifiedResponse, FileRole}
import play.mvc.Http.Status
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsFinancialsApiConnector @Inject()(appConfig: AppConfig,
                                              httpClient: HttpClientV2,
                                              metricsReporter: MetricsReporterService)(implicit ec: ExecutionContext) {

  def isEmailVerified(implicit hc: HeaderCarrier): Future[EmailVerifiedResponse] = {
    httpClient.get(url"${appConfig.customsFinancialsApi}/subscriptions/subscriptionsdisplay")
      .execute[EmailVerifiedResponse]
      .flatMap {
        Future.successful
      }
    //httpClient.GET[EmailVerifiedResponse](appConfig.customsFinancialsApi + "/subscriptions/subscriptionsdisplay")
  }

  def getEmailaddress(implicit hc: HeaderCarrier): Future[EmailVerifiedResponse] = {
    httpClient.get(url"${appConfig.customsFinancialsApi}/subscriptions/email-display")
      .execute[EmailVerifiedResponse]
      .flatMap {
        Future.successful
      }
    //httpClient.GET[EmailVerifiedResponse](appConfig.customsFinancialsApi + "/subscriptions/email-display")
  }

  def isEmailUnverified(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpClient.get(url"${appConfig.customsFinancialsApi}/subscriptions/unverified-email-display")
      .execute[EmailUnverifiedResponse]
      .flatMap {
        res => Future.successful(res.unVerifiedEmail)
      }
   /* httpClient.GET[EmailUnverifiedResponse](appConfig.customsFinancialsApi +
      "/subscriptions/unverified-email-display").map(res => res.unVerifiedEmail)*/
  }

  def deleteNotification(eori: String,
                         fileRole: FileRole)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val apiEndpoint = s"${appConfig.customsFinancialsApi}/eori/$eori/notifications/$fileRole"

    metricsReporter.withResponseTimeLogging("customs-financials-api.delete.notification") {
      httpClient.delete(url"$apiEndpoint")
        .execute[HttpResponse]
        .flatMap {
          res => Future.successful(res.status == Status.OK)
        }
      //httpClient.DELETE[HttpResponse](apiEndpoint).map(_.status == Status.OK)
    }
  }
}

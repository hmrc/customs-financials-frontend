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
import domain.FileRole
import play.mvc.Http.Status
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsFinancialsApiConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  metricsReporter: MetricsReporterService
)(implicit ec: ExecutionContext) {

  def deleteNotification(eori: String, fileRole: FileRole)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val apiEndpoint = s"${appConfig.customsFinancialsApi}/eori/$eori/notifications/$fileRole"

    metricsReporter.withResponseTimeLogging("customs-financials-api.delete.notification") {
      httpClient
        .delete(url"$apiEndpoint")
        .execute[HttpResponse]
        .flatMap { res =>
          Future.successful(res.status == Status.OK)
        }
    }
  }
}

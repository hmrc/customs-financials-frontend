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
import domain.{AccountLink, AccountLinkWithoutDate, SessionCacheAccountLink}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import play.api.libs.ws.BodyWritable
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpReadsInstances.readFromJson
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class AccountLinksRequest(sessionId: String, accountLinks: Seq[SessionCacheAccountLink])

object AccountLinksRequest {
  implicit val format: OFormat[AccountLinksRequest] = Json.format[AccountLinksRequest]

  implicit def jsonBodyWritable[T](implicit
    writes: Writes[T],
    jsValueBodyWritable: BodyWritable[JsValue]
  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}

class CustomsFinancialsSessionCacheConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  metricsReporter: MetricsReporterService
)(implicit executionContext: ExecutionContext) {

  def storeSession(id: String, accountLinks: Seq[AccountLink])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val sessionCacheUrl = s"${appConfig.customsFinancialsSessionCacheUrl}/update-links"

    metricsReporter.withResponseTimeLogging("customs-financials-session-cache.update-links") {
      val request: AccountLinksRequest = AccountLinksRequest(id, toSessionCacheAccountLinks(accountLinks))

      httpClient
        .post(url"$sessionCacheUrl")
        .withBody[AccountLinksRequest](request)
        .execute[HttpResponse]
        .flatMap {
          Future.successful
        }
    }
  }

  def getAccontLinks(sessionId: String)(implicit hc: HeaderCarrier): Future[Option[Seq[AccountLinkWithoutDate]]] =
    httpClient
      .get(url"${appConfig.customsFinancialsSessionCacheUrl}/account-links/$sessionId")
      .execute[Seq[AccountLinkWithoutDate]]
      .flatMap { res =>
        Future.successful(Some(res))
      }
      .recover { case _ =>
        None
      }

  def getSessionId(sessionId: String)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] =
    httpClient
      .get(url"${appConfig.customsFinancialsSessionCacheUrl}/account-links/session/$sessionId")
      .execute[HttpResponse]
      .flatMap { res =>
        Future.successful(Some(res))
      }
      .recover { case _ =>
        None
      }

  def removeSession(id: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val sessionCacheUrl = s"${appConfig.customsFinancialsSessionCacheUrl}/remove/$id"

    metricsReporter.withResponseTimeLogging("customs-financials-session-cache.remove") {
      httpClient
        .delete(url"$sessionCacheUrl")
        .execute[HttpResponse]
        .flatMap {
          Future.successful
        }
    }
  }

  private def toSessionCacheAccountLinks(accountLinks: Seq[AccountLink]): Seq[SessionCacheAccountLink] = for {
    accountLink       <- accountLinks
    sessionAccountLink = SessionCacheAccountLink(
                           accountLink.eori,
                           accountLink.isNiAccount,
                           accountLink.accountNumber,
                           accountLink.accountStatus,
                           accountLink.accountStatusId,
                           accountLink.linkId
                         )
  } yield sessionAccountLink
}

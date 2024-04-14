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
import play.api.libs.json.{Json, OFormat}
import services.MetricsReporterService
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readFromJson
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class AccountLinksRequest(sessionId: String,
                               accountLinks: Seq[SessionCacheAccountLink])

object AccountLinksRequest {
  implicit val format: OFormat[AccountLinksRequest] = Json.format[AccountLinksRequest]
}


class CustomsFinancialsSessionCacheConnector @Inject()(httpClient: HttpClient,
                                                       appConfig: AppConfig,
                                                       metricsReporter: MetricsReporterService)
                                                      (implicit executionContext: ExecutionContext) {

  def storeSession(id: String, accountLinks: Seq[AccountLink])(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val sessionCacheUrl = appConfig.customsFinancialsSessionCacheUrl + "/update-links"

    metricsReporter.withResponseTimeLogging("customs-financials-session-cache.update-links") {
      val request: AccountLinksRequest = AccountLinksRequest(id, toSessionCacheAccountLinks(accountLinks))

      httpClient.POST[AccountLinksRequest, HttpResponse](sessionCacheUrl, request)
    }
  }

  def getAccontLinks(sessionId: String)(implicit hc: HeaderCarrier): Future[Option[Seq[AccountLinkWithoutDate]]] =
    httpClient.GET[Seq[AccountLinkWithoutDate]](
      appConfig.customsFinancialsSessionCacheUrl + s"/account-links/$sessionId").map(
      Some(_)).recover { case _ => None }

  def getSessionId(sessionId: String)(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] =
    httpClient.GET[HttpResponse](
      appConfig.customsFinancialsSessionCacheUrl + s"/account-links/session/$sessionId").map(
      Some(_)).recover { case _ => None }

  def removeSession(id: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val sessionCacheUrl = appConfig.customsFinancialsSessionCacheUrl + "/remove/" + id

    metricsReporter.withResponseTimeLogging("customs-financials-session-cache.remove") {
      httpClient.DELETE[HttpResponse](sessionCacheUrl)
    }
  }

  private def toSessionCacheAccountLinks(accountLinks: Seq[AccountLink]): Seq[SessionCacheAccountLink] = for {
    accountLink <- accountLinks
    sessionAccountLink = SessionCacheAccountLink(accountLink.eori,
      accountLink.isNiAccount,
      accountLink.accountNumber,
      accountLink.accountStatus,
      accountLink.accountStatusId,
      accountLink.linkId)
  } yield sessionAccountLink
}

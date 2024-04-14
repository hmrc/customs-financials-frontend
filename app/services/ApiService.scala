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

package services

import config.AppConfig
import domain._
import play.api.libs.json.{JsResultException, Json, OFormat}
import play.api.{Logger, LoggerLike}
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class SdesNotificationsForEori(eori: String,
                                    notifications: Seq[DocumentAttributes])

case class DocumentAttributes(eori: String,
                              fileRole: FileRole,
                              fileName: String,
                              fileSize: Long,
                              metadata: Map[String, String]) {

  lazy val isRequested: Boolean = metadata.isDefinedAt("statementRequestID")
}

object SdesNotificationsForEori {
  implicit val sdesNotificationFormat: OFormat[DocumentAttributes] = Json.format[DocumentAttributes]
  implicit val sdesNotificationsFormat: OFormat[SdesNotificationsForEori] = Json.format[SdesNotificationsForEori]

  val requested = true
  val regular = false
}

@Singleton
class ApiService @Inject()(http: HttpClient, metricsReporter: MetricsReporterService)
                          (implicit appConfig: AppConfig, ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def getAccounts(eori: String)(implicit hc: HeaderCarrier): Future[CDSAccounts] = {
    val apiEndpoint = appConfig.customsFinancialsApi + s"/eori/accounts"

    val requestDetail = AccountsRequestDetail(eori, None, None, None)
    val accountsAndBalancesRequest = AccountsAndBalancesRequestContainer(
      domain.AccountsAndBalancesRequest(AccountsRequestCommon.generate, requestDetail)
    )
    metricsReporter.withResponseTimeLogging("customs-financials-api.get.accounts") {
      http.POST[AccountsAndBalancesRequestContainer, AccountsAndBalancesResponseContainer](
        apiEndpoint, accountsAndBalancesRequest).map(_.toCdsAccounts(eori))
    }
  }

  def searchAuthorities(eori: String,
                        searchID: String)
                       (implicit hc: HeaderCarrier): Future[Either[SearchResponse, SearchedAuthorities]] = {
    val apiEndpoint = appConfig.customsFinancialsApi + "/search-authorities"
    val request = SearchAuthoritiesRequest(searchID, eori)

    metricsReporter.withResponseTimeLogging("customs-financials-api.get.search-authorities") {
      http.POST[SearchAuthoritiesRequest, HttpResponse](apiEndpoint, request).map {
        case response if response.status == Status.NO_CONTENT => Left(NoAuthorities)
        case response if response.status != Status.OK => Left(SearchError)
        case response => Json.parse(response.body).asOpt[SearchedAuthoritiesResponse] match {
          case Some(value) => Right(value.toSearchAuthorities)
          case None => Left(SearchError)
        }
      }.recover { case _ => Left(SearchError) }
    }
  }

  def getEnabledNotifications(eori: String)(implicit hc: HeaderCarrier): Future[Seq[DocumentAttributes]] = {
    val apiEndpoint = appConfig.customsFinancialsApi + s"/eori/$eori/notifications"

    metricsReporter.withResponseTimeLogging("customs-financials-api.get.notifications") {
      http.GET[SdesNotificationsForEori](apiEndpoint).map(_.notifications)
    }
  }

  def deleteNotification(eori: String, fileRole: FileRole)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val apiEndpoint = appConfig.customsFinancialsApi + s"/eori/$eori/notifications/$fileRole"

    metricsReporter.withResponseTimeLogging("customs-financials-api.delete.notification") {
      http.DELETE[HttpResponse](apiEndpoint).map(_.status == Status.OK)
    }
  }

  def requestAuthoritiesCsv(eori: String, alternateEORI: Option[String])(
    implicit hc: HeaderCarrier): Future[Either[RequestCsvResponse, RequestAuthoritiesCsvResponse]] = {

    val apiEndpoint = appConfig.customsFinancialsApi + "/standing-authorities-file"
    val requestAuthoritiesCsv: RequestAuthoritiesCsv = RequestAuthoritiesCsv(eori, alternateEORI)

    metricsReporter.withResponseTimeLogging("customs-financials-api.request.authorities.csv") {
      http.POST[RequestAuthoritiesCsv, HttpResponse](apiEndpoint, requestAuthoritiesCsv).map {
        case response if response.status == Status.OK =>
          Json.parse(response.body).as[RequestAuthoritiesCsvResponse] match {
            case value => Right(value)
          }

        case response =>
          log.error(s"requestAuthoritiesCsv failed with ${response.status} ${response.body}")
          Left(RequestAuthoritiesCSVError)

      }.recover {
        case ex: JsResultException =>
          log.error(s"requestAuthoritiesCsv threw an JS exception - ${ex.getMessage}")
          Left(JsonParseError)
        case ex: Throwable =>
          log.error(s"requestAuthoritiesCsv threw an exception - ${ex.getMessage}")
          Left(RequestAuthoritiesCSVError)
      }
    }
  }
}

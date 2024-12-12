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
import domain.*
import play.api.libs.json.{JsResultException, Json, OFormat}
import play.api.{Logger, LoggerLike}
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class SdesNotificationsForEori(eori: String, notifications: Seq[DocumentAttributes])

case class DocumentAttributes(
  eori: String,
  fileRole: FileRole,
  fileName: String,
  fileSize: Long,
  metadata: Map[String, String]
) {

  lazy val isRequested: Boolean = metadata.isDefinedAt("statementRequestID")
}

object SdesNotificationsForEori {
  implicit val sdesNotificationFormat: OFormat[DocumentAttributes]        = Json.format[DocumentAttributes]
  implicit val sdesNotificationsFormat: OFormat[SdesNotificationsForEori] = Json.format[SdesNotificationsForEori]

  val requested = true
}

@Singleton
class ApiService @Inject() (httpClient: HttpClientV2, metricsReporter: MetricsReporterService)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
) {

  val log: LoggerLike = Logger(this.getClass)

  def getAccounts(eori: String)(implicit hc: HeaderCarrier): Future[CDSAccounts] = {
    val apiEndpoint = s"${appConfig.customsFinancialsApi}/eori/accounts"

    val requestDetail              = AccountsRequestDetail(eori, None, None, None)
    val accountsAndBalancesRequest = AccountsAndBalancesRequestContainer(
      domain.AccountsAndBalancesRequest(AccountsRequestCommon.generate, requestDetail)
    )

    metricsReporter.withResponseTimeLogging("customs-financials-api.get.accounts") {
      httpClient
        .post(url"$apiEndpoint")
        .withBody[AccountsAndBalancesRequestContainer](accountsAndBalancesRequest)
        .execute[AccountsAndBalancesResponseContainer]
        .flatMap { res =>
          Future.successful(res.toCdsAccounts(eori))
        }
    }
  }

  def searchAuthorities(eori: String, searchID: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[SearchResponse, SearchedAuthorities]] = {
    val apiEndpoint = s"${appConfig.customsFinancialsApi}/search-authorities"
    val request     = SearchAuthoritiesRequest(searchID, eori)

    metricsReporter.withResponseTimeLogging("customs-financials-api.get.search-authorities") {
      httpClient
        .post(url"$apiEndpoint")
        .withBody[SearchAuthoritiesRequest](request)
        .execute[HttpResponse]
        .flatMap {
          case response if response.status == Status.NO_CONTENT => Future.successful(Left(NoAuthorities))
          case response if response.status != Status.OK         => Future.successful(Left(SearchError))
          case response                                         =>
            Json.parse(response.body).asOpt[SearchedAuthoritiesResponse] match {
              case Some(value) => Future.successful(Right(value.toSearchAuthorities))
              case None        => Future.successful(Left(SearchError))
            }
        }
        .recover { case _ => Left(SearchError) }
    }
  }

  def getEnabledNotifications(eori: String)(implicit hc: HeaderCarrier): Future[Seq[DocumentAttributes]] = {
    val apiEndpoint = appConfig.customsFinancialsApi + s"/eori/$eori/notifications"

    metricsReporter.withResponseTimeLogging("customs-financials-api.get.notifications") {
      httpClient
        .get(url"$apiEndpoint")
        .execute[SdesNotificationsForEori]
        .flatMap { res =>
          Future.successful(res.notifications)
        }
    }
  }

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

  def requestAuthoritiesCsv(eori: String, alternateEORI: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[RequestCsvResponse, RequestAuthoritiesCsvResponse]] = {

    val apiEndpoint                                  = s"${appConfig.customsFinancialsApi}/standing-authorities-file"
    val requestAuthoritiesCsv: RequestAuthoritiesCsv = RequestAuthoritiesCsv(eori, alternateEORI)

    metricsReporter.withResponseTimeLogging("customs-financials-api.request.authorities.csv") {
      httpClient
        .post(url"$apiEndpoint")
        .withBody[RequestAuthoritiesCsv](requestAuthoritiesCsv)
        .execute[HttpResponse]
        .flatMap {
          case response if response.status == Status.OK =>
            Json.parse(response.body).as[RequestAuthoritiesCsvResponse] match {
              case value => Future.successful(Right(value))
            }

          case response =>
            log.error(s"requestAuthoritiesCsv failed with ${response.status} ${response.body}")
            Future.successful(Left(RequestAuthoritiesCSVError))
        }
        .recover {
          case ex: JsResultException =>
            log.error(s"requestAuthoritiesCsv threw an JS exception - ${ex.getMessage}")
            Left(JsonParseError)
          case ex: Throwable         =>
            log.error(s"requestAuthoritiesCsv threw an exception - ${ex.getMessage}")
            Left(RequestAuthoritiesCSVError)
        }
    }
  }
}

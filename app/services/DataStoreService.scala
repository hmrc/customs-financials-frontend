/*
 * Copyright 2022 HM Revenue & Customs
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

import domain.CompanyAddress
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.auth.core.retrieve.Email
import config.AppConfig
import domain.{CompanyAddress, EORI, EmailResponses, EoriHistory, UndeliverableEmail, UndeliverableInformation, UnverifiedEmail}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataStoreService @Inject()(http: HttpClient, metricsReporter: MetricsReporterService)(implicit appConfig: AppConfig, ec: ExecutionContext) {

  val log = Logger(this.getClass)

  def getAllEoriHistory(eori: EORI)(implicit hc: HeaderCarrier): Future[Seq[EoriHistory]] = {
    val dataStoreEndpoint = appConfig.customsDataStore + s"/eori/$eori/eori-history"
    val emptyEoriHistory = Seq(EoriHistory(eori, None, None))
    metricsReporter.withResponseTimeLogging("customs-data-store.get.eori-history") {
      http.GET[EoriHistoryResponse](dataStoreEndpoint).map(response => response.eoriHistory)
        .recover { case e =>
          log.error(s"DATASTORE-E-EORI-HISTORY-ERROR: ${e.getClass.getName}")
          emptyEoriHistory
        }
    }
  }

  def getEmail(eori: String)(implicit hc: HeaderCarrier): Future[Either[EmailResponses, Email]] = {
    val dataStoreEndpoint = appConfig.customsDataStore + s"/eori/$eori/verified-email"
    metricsReporter.withResponseTimeLogging("customs-data-store.get.email") {
      http.GET[EmailResponse](dataStoreEndpoint).map {
        case EmailResponse(Some(address), _, None) => Right(Email(address))
        case EmailResponse(Some(email), _, Some(_)) => Left(UndeliverableEmail(email))
        case _ => Left(UnverifiedEmail)
      }.recover {
        case UpstreamErrorResponse(_, NOT_FOUND, _, _) => Left(UnverifiedEmail)
      }
    }
  }

  def getCompanyName(eori: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val dataStoreEndpoint = appConfig.customsDataStore + s"/eori/$eori/company-information"
    metricsReporter.withResponseTimeLogging("customs-data-store.get.company-information") {
      http.GET[CompanyInformationResponse](dataStoreEndpoint).map(response => Some(response.name))
    }.recover { case e =>
      log.error(s"Call to data stored failed url=$dataStoreEndpoint, exception=$e")
      None
    }
  }
}

case class EmailResponse(address: Option[String], timestamp: Option[String], undeliverable: Option[UndeliverableInformation])

object EmailResponse {
  implicit val format: OFormat[EmailResponse] = Json.format[EmailResponse]
}

case class EoriHistoryResponse(eoriHistory: Seq[EoriHistory])

object EoriHistoryResponse {
  implicit val format: OFormat[EoriHistoryResponse] = Json.format[EoriHistoryResponse]
}

case class CompanyInformationResponse(name: String, address: CompanyAddress)

object CompanyInformationResponse {
  implicit val format: OFormat[CompanyInformationResponse] = Json.format[CompanyInformationResponse]
}

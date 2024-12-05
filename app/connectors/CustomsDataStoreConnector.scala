/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import domain.{EmailUnverifiedResponse, EmailVerifiedResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
    ec: ExecutionContext
) {

  def isEmailVerified(implicit hc: HeaderCarrier): Future[EmailVerifiedResponse] = {
    httpClient
      .get(url"${appConfig.customsDataStore}/subscriptions/subscriptionsdisplay")
      .execute[EmailVerifiedResponse]
      .flatMap { response =>
        Future.successful(response)
      }
  }

  def getEmailAddress(implicit hc: HeaderCarrier): Future[EmailVerifiedResponse] = {
    httpClient
      .get(url"${appConfig.customsDataStore}/subscriptions/email-display")
      .execute[EmailVerifiedResponse]
      .flatMap { response =>
        Future.successful(response)
      }
  }

  def isEmailUnverified(implicit hc: HeaderCarrier): Future[Option[String]] = {
    httpClient
      .get(url"${appConfig.customsDataStore}/subscriptions/unverified-email-display")
      .execute[EmailUnverifiedResponse]
      .flatMap { res =>
        Future.successful(res.unVerifiedEmail)
      }
  }
}

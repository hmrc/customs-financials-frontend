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
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionCacheConnector @Inject()(httpClient: HttpClient,
  appConfig: AppConfig)(implicit executionContext: ExecutionContext) {

  def getAccountNumbers(eori: String, sessionId: String)(implicit hc: HeaderCarrier): Future[Option[Seq[String]]] =
    httpClient.GET[Seq[String]](appConfig.customsSessionCacheUrl + s"/account-numbers/$eori/$sessionId"
    ).map(Some(_)).recover { case _ => None }
}

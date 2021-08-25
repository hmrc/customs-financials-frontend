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

package uk.gov.hmrc.customs.financials.connectors

import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.Future

class SDDSConnectorSpec extends SpecBase with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  "startJourney" should {
    "return a valid redirect url when a valid DAN is provided" in new Setup {

      running (app){
        val connector = app.injector.instanceOf[SDDSConnector]

        val result: Future[String] = connector.startJourney(returnUrl, backUrl, dan, email)
        await(result) mustBe nextUrl
      }
    }
  }

  trait Setup {

    val dan = "DAN123456"
    val email = "test@email.com"
    val baseUrl = "sddsBaseUrl"
    val expectedUrl = baseUrl
    val returnUrl = "/return-url"
    val backUrl = "/back-url"
    val nextUrl = "/next-url"

    private val mockAppConfig = mock[AppConfig]
    private val mockHttpClient = mock[HttpClient]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val expectedRequest: SDDSRequest = SDDSRequest(returnUrl, backUrl, dan, email)
    val expectedResponse: SDDSResponse = SDDSResponse(nextUrl)

    when[Future[SDDSResponse]](mockHttpClient.POST(eqTo(expectedUrl), eqTo(expectedRequest), any)(any, any, any, any))
      .thenReturn(Future.successful(expectedResponse))

    when(mockAppConfig.sddsUri).thenReturn(baseUrl)

    val app = application().overrides(
      bind[AppConfig].toInstance(mockAppConfig),
      bind[HttpClient].toInstance(mockHttpClient)
    ).build()


  }
}

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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SERVICE_UNAVAILABLE}
import play.api.test.Helpers.{running, status}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Application, inject}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.SpecBase
import utils.TestData.TEST_EORI
import utils.MustMatchers

import scala.concurrent.{ExecutionContext, Future}
import java.net.URL

class CustomsManageAuthoritiesConnectorSpec extends SpecBase
  with ScalaFutures
  with FutureAwaits
  with DefaultAwaitTimeout
  with MustMatchers {

  "fetchAndSaveAccountAuthoritiesInCache" should {

    "return the correct result" when {

      "API call to fetch authorities is successful" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsManageAuthoritiesConnector]

          when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
            .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

          when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

          val result = connector.fetchAndSaveAccountAuthoritiesInCache(TEST_EORI)(fakeRequest())
          status(result) mustBe OK
        }
      }

      "API call to fetch authorities returns NO_CONTENT" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsManageAuthoritiesConnector]

          when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
            .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, emptyString)))

          when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

          val result = connector.fetchAndSaveAccountAuthoritiesInCache(TEST_EORI)(fakeRequest())
          status(result) mustBe OK
        }
      }

      "API call to fetch authorities returns INTERNAL_SERVER_ERROR" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsManageAuthoritiesConnector]

          when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
            .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, emptyString)))

          when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

          val result = connector.fetchAndSaveAccountAuthoritiesInCache(TEST_EORI)(fakeRequest())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }

      "API call to fetch authorities returns SERVICE_UNAVAILABLE" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsManageAuthoritiesConnector]

          when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
            .thenReturn(Future.successful(HttpResponse.apply(SERVICE_UNAVAILABLE, emptyString)))

          when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

          val result = connector.fetchAndSaveAccountAuthoritiesInCache(TEST_EORI)(fakeRequest())
          status(result) mustBe SERVICE_UNAVAILABLE
        }
      }

      "API call to fetch authorities throws exception" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsManageAuthoritiesConnector]

          when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
            .thenReturn(Future.failed(UpstreamErrorResponse("Internal Error", INTERNAL_SERVER_ERROR)))

          when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

          val result = connector.fetchAndSaveAccountAuthoritiesInCache(TEST_EORI)(fakeRequest())
          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  trait Setup {
    val mockAppConfig: AppConfig = mock[AppConfig]
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    val endPointUrl =
      s"http://localhost:9000/customs/manage-authorities/account-authorities/fetch-authorities/$TEST_EORI"

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app: Application = application().overrides(
      inject.bind[AppConfig].toInstance(mockAppConfig),
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[RequestBuilder].toInstance(requestBuilder)
    ).build()
  }
}

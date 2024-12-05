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

package controllers

import connectors.CustomsDataStoreConnector
import domain.{EmailUnverifiedResponse, EmailVerifiedResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers.*

import java.net.URL
import services.MetricsReporterService
import utils.SpecBase
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, *}
import utils.MustMatchers

class EmailControllerSpec extends SpecBase with MustMatchers {

  "EmailController" should {
    "return unverified email" in new Setup {

      running(app) {
        val connector = app.injector.instanceOf[CustomsDataStoreConnector]

        val result: Future[Option[String]] = connector.isEmailUnverified(hc)
        await(result) mustBe expectedResult
      }
    }

    "return undeliverable email" in new Setup {
      val undeliverableResponse: EmailVerifiedResponse = EmailVerifiedResponse(Some("undeliverbaleEmail"))
      val expectedUndeliverabeResult: EmailVerifiedResponse = EmailVerifiedResponse(Some("undeliverbaleEmail"))
      val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
      val requestBuilder: RequestBuilder = mock[RequestBuilder]

      when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(undeliverableResponse))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      override val app: Application = application()
        .overrides(
          bind[MetricsReporterService].toInstance(mockMetricsReporterService),
          bind[HttpClientV2].toInstance(mockHttpClient),
          bind[RequestBuilder].toInstance(requestBuilder)
        )
        .build()

      val connector: CustomsDataStoreConnector = app.injector.instanceOf[CustomsDataStoreConnector]

      val result: Future[EmailVerifiedResponse] = connector.isEmailVerified(hc)

      await(result) mustBe expectedUndeliverabeResult

    }

    "return unverified email response" in new Setup {
      running(app) {

        val request = fakeRequest(GET, routes.EmailController.showUnverified().url)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "return undeliverable email response" in {

      val mockHttpClient = mock[HttpClientV2]
      val requestBuilder = mock[RequestBuilder]
      val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]
      val response = EmailVerifiedResponse(Some("undeliverableEmail"))

      when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      val app = application()
        .overrides(
          bind[MetricsReporterService].toInstance(mockMetricsReporterService),
          bind[HttpClientV2].toInstance(mockHttpClient),
          bind[RequestBuilder].toInstance(requestBuilder)
        )
        .build()

      val request = fakeRequest(GET, routes.EmailController.showUndeliverable().url)
      val result = route(app, request).value

      status(result) mustBe OK
    }
  }

  trait Setup {
    val expectedResult: Option[String] = Some("unverifiedEmail")
    implicit val hc: HeaderCarrier = HeaderCarrier()
    private val mockHttpClient = mock[HttpClientV2]
    private val requestBuilder = mock[RequestBuilder]
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    val response: EmailUnverifiedResponse = EmailUnverifiedResponse(Some("unverifiedEmail"))

    when(requestBuilder.execute(any[HttpReads[EmailUnverifiedResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(response))

    when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

    val app: Application = application()
      .overrides(
        bind[MetricsReporterService].toInstance(mockMetricsReporterService),
        bind[HttpClientV2].toInstance(mockHttpClient),
        bind[RequestBuilder].toInstance(requestBuilder)
      )
      .build()
  }
}

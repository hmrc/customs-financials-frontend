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

import connectors.CustomsFinancialsApiConnector
import domain.{EmailUnverifiedResponse, EmailVerifiedResponse}
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers._
import services.MetricsReporterService
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.Future

class EmailControllerSpec extends SpecBase {

  "EmailController" should {
    "return unverified email" in new Setup {

      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsApiConnector]

        val result: Future[Option[String]] = connector.isEmailUnverified(hc)
        await(result) mustBe expectedResult
      }
    }

    "return undeliverable email" in new Setup {
      val undeliverableResponse: EmailVerifiedResponse = EmailVerifiedResponse(Some("undeliverbaleEmail"))
      val expectedUndeliverabeResult: EmailVerifiedResponse = EmailVerifiedResponse(Some("undeliverbaleEmail"))
      val mockHttpClient: HttpClient = mock[HttpClient]

      when[Future[EmailVerifiedResponse]](mockHttpClient.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(undeliverableResponse))

      override val app: Application = application().overrides(
        bind[MetricsReporterService].toInstance(mockMetricsReporterService),
        bind[HttpClient].toInstance(mockHttpClient)
      ).build()


      val connector: CustomsFinancialsApiConnector = app.injector.instanceOf[CustomsFinancialsApiConnector]

      val result: Future[EmailVerifiedResponse] = connector.isEmailVerified(hc)

      await(result) mustBe expectedUndeliverabeResult

    }

    "return unverified email response" in new Setup {
      running(app) {

        val request = fakeRequest(GET, routes.EmailController.showUnverified().url)
        val result = route(app, request).value
        status(result) shouldBe OK
      }
    }

    "return undeliverable email response" in {

      val mockHttpClient = mock[HttpClient]
      val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]
      val response = EmailVerifiedResponse(Some("undeliverableEmail"))

      when[Future[EmailVerifiedResponse]](mockHttpClient.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(response))
      val app = application().overrides(
        bind[MetricsReporterService].toInstance(mockMetricsReporterService),
        bind[HttpClient].toInstance(mockHttpClient)
      ).build()

      val request = fakeRequest(GET, routes.EmailController.showUndeliverable().url)
      val result = route(app, request).value

      status(result) shouldBe OK
    }
  }

  trait Setup {
    val expectedResult: Option[String] = Some("unverifiedEmail")
    implicit val hc: HeaderCarrier = HeaderCarrier()
    private val mockHttpClient = mock[HttpClient]
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    val response: EmailUnverifiedResponse = EmailUnverifiedResponse(Some("unverifiedEmail"))

    when[Future[EmailUnverifiedResponse]](mockHttpClient.GET(any, any, any)(any, any, any))
      .thenReturn(Future.successful(response))

    val app: Application = application().overrides(
      bind[MetricsReporterService].toInstance(mockMetricsReporterService),
      bind[HttpClient].toInstance(mockHttpClient)
    ).build()
  }
}

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

package connectors

import domain.EmailVerifiedResponse
import domain.FileRole.StandingAuthority
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.bind
import play.api.test.Helpers.{await, _}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import services.MetricsReporterService
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.Future

class CustomsFinancialApiConnectorSpec extends SpecBase with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  "CustomsFinancialApiConnector" should {
    "return verified email" in new Setup {

      running (app){
        val connector = app.injector.instanceOf[CustomsFinancialsApiConnector]

        val result: Future[EmailVerifiedResponse] = connector.isEmailVerified(hc)
        await(result) mustBe expectedResult
      }
    }

    "delete notifications should return a boolean based on the result" in new Setup {

      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsApiConnector]

        val result = await(connector.deleteNotification("someEori", StandingAuthority))
        result mustBe true
      }
    }

  }

  trait Setup {

    val expectedResult = EmailVerifiedResponse(Some("verifiedEmail"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    private val mockHttpClient = mock[HttpClient]
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    val response = EmailVerifiedResponse(Some("verifiedEmail"))

    when[Future[EmailVerifiedResponse]](mockHttpClient.GET(any, any, any)(any, any, any))
      .thenReturn(Future.successful(response))

    when(mockMetricsReporterService.withResponseTimeLogging[HttpResponse](any)(any)(any))
      .thenAnswer((i: InvocationOnMock) => {
        i.getArgument[Future[HttpResponse]](1)
      })

    when[Future[HttpResponse]](mockHttpClient.DELETE(any, any)(any, any, any))
      .thenReturn(Future.successful(HttpResponse(200, "")))

    val app = application().overrides(
      bind[MetricsReporterService].toInstance(mockMetricsReporterService),
      bind[HttpClient].toInstance(mockHttpClient)
    ).build()


  }
}

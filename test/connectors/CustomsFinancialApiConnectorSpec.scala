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

import domain.FileRole.StandingAuthority
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import services.MetricsReporterService
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.MustMatchers

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class CustomsFinancialApiConnectorSpec
    extends SpecBase
    with ScalaFutures
    with FutureAwaits
    with DefaultAwaitTimeout
    with MustMatchers {

  "CustomsFinancialApiConnector" should {

    "delete notifications should return a boolean based on the result" in new Setup {

      running(app) {

        val result = await(customsFinancialsApiConnector.deleteNotification("someEori", StandingAuthority))
        result mustBe true
      }
    }
  }

  trait Setup {

    implicit val hc: HeaderCarrier                         = HeaderCarrier()
    val mockHttpClient: HttpClientV2                       = mock[HttpClientV2]
    val requestBuilder: RequestBuilder                     = mock[RequestBuilder]
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

    when(mockMetricsReporterService.withResponseTimeLogging[HttpResponse](any)(any)(any))
      .thenAnswer { (i: InvocationOnMock) =>
        i.getArgument[Future[HttpResponse]](1)
      }

    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
    when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(HttpResponse(OK, emptyString)))

    when(mockHttpClient.delete(any[URL]())(any())).thenReturn(requestBuilder)

    val app: Application = application()
      .overrides(
        bind[MetricsReporterService].toInstance(mockMetricsReporterService),
        bind[HttpClientV2].toInstance(mockHttpClient),
        bind[RequestBuilder].toInstance(requestBuilder)
      )
      .build()

    val customsFinancialsApiConnector: CustomsFinancialsApiConnector =
      app.injector.instanceOf[CustomsFinancialsApiConnector]
  }
}

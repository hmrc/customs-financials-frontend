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
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.servicenavigation.ServiceNavigationItem
import uk.gov.hmrc.http.HttpReads
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.SpecBase
import utils.MustMatchers

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class SecureMessageConnectorSpec extends SpecBase with MustMatchers {

  "getMessageCountBanner" should {

    "return a valid message banner when upstream call succeeds" in new Setup {

      when(mockHttpClient.get(any[URL]())(any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.transform(any()))
        .thenReturn(requestBuilder)

      when(
        requestBuilder.execute(
          any[HttpReads[Seq[ServiceNavigationItem]]],
          any[ExecutionContext]
        )
      ).thenReturn(
        Future.successful(
          Seq(
            ServiceNavigationItem(
              content = Text("Hello"),
              href = "/test"
            )
          )
        )
      )

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result =
          await(connector.getMessageCountBanner(returnTo)(fakeRequest()))

        result mustBe Some(
          Seq(
            ServiceNavigationItem(
              content = Text("Hello"),
              href = "/test"
            )
          )
        )
      }
    }

    "return None when upstream call fails" in new Setup {

      when(mockHttpClient.get(any[URL]())(any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.transform(any()))
        .thenReturn(requestBuilder)

      when(
        requestBuilder.execute(
          any[HttpReads[Seq[ServiceNavigationItem]]],
          any[ExecutionContext]
        )
      ).thenReturn(
        Future.failed(new RuntimeException("boom"))
      )

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result =
          await(connector.getMessageCountBanner(returnTo)(fakeRequest()))

        result mustBe None
      }
    }
  }

  trait Setup {

    protected val mockHttpClient: HttpClientV2   = mock[HttpClientV2]
    protected val requestBuilder: RequestBuilder = mock[RequestBuilder]

    protected val returnTo = "backhere.com"

    protected val app: Application =
      application()
        .overrides(bind[HttpClientV2].toInstance(mockHttpClient))
        .build()

    protected implicit val ec: ExecutionContext =
      scala.concurrent.ExecutionContext.global

    protected val appConfig: AppConfig =
      app.injector.instanceOf[AppConfig]
  }
}

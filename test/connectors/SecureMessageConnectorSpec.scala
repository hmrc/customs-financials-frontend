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

import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers.*
import play.twirl.api.Html
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import utils.SpecBase
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.partials.HtmlPartial
import uk.gov.hmrc.http.{HttpReads, *}
import utils.MustMatchers
import java.net.URL

import scala.concurrent.{ExecutionContext, Future}

class SecureMessageConnectorSpec extends SpecBase with MustMatchers {

  "getMessageCountBanner" should {

    "return a valid message banner, when the upstream call returns OK" in new Setup {
      when(requestBuilder.execute(any[HttpReads[HtmlPartial]], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Success(Some("Hello"), Html(emptyString))))

      when(requestBuilder.transform(any())).thenReturn(requestBuilder)

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.get mustEqual HtmlPartial.Success(Some("Hello"), Html(emptyString))
      }
    }

    "return None when the upstream call throws an Exception" in new Setup {
      when(requestBuilder.execute(any[HttpReads[HtmlPartial]], any[ExecutionContext]))
        .thenReturn(Future.failed(new Exception("ahh")))

      when(requestBuilder.transform(any())).thenReturn(requestBuilder)

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.isEmpty mustEqual true
      }
    }

    "return None when the upstream call returns an unhappy response" in new Setup {
      when(requestBuilder.execute(any[HttpReads[HtmlPartial]], any[ExecutionContext]))
        .thenReturn(Future.successful(HtmlPartial.Failure(Some(INTERNAL_SERVER_ERROR), "ahh")))

      when(requestBuilder.transform(any())).thenReturn(requestBuilder)

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.isEmpty mustEqual true
      }
    }

  }

  trait Setup {

    protected val mockHttpClient: HttpClientV2   = mock[HttpClientV2]
    protected val requestBuilder: RequestBuilder = mock[RequestBuilder]
    protected val expectedUrl                    = "messageBannerEndpoint"
    protected val returnTo                       = "backhere.com"

    protected val app: Application = application()
      .overrides(bind[HttpClientV2].toInstance(mockHttpClient))
      .overrides(bind[RequestBuilder].toInstance(requestBuilder))
      .build()
  }
}

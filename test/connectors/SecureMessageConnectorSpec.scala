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

import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers._
import play.twirl.api.Html
import config.AppConfig
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import utils.SpecBase
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.Future

class SecureMessageConnectorSpec extends SpecBase {

  "getMessageCountBanner" should {

    "return a valid message banner, when the upstream call returns OK" in new Setup {
      when(mockConfig.customsSecureMessagingBannerEndpoint).thenReturn(expectedUrl)

      when[Future[HtmlPartial]](mockHttpClient.GET(eqTo(expectedUrl),any, any)(any, any, any))
        .thenReturn(Future.successful(HtmlPartial.Success(Some("Hello"),Html(""))))

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.get mustEqual HtmlPartial.Success(Some("Hello"),Html(""))
      }
    }

    "return None when the upstream call throws an Exception" in new Setup {
      when(mockConfig.customsSecureMessagingBannerEndpoint).thenReturn(expectedUrl)

      when[Future[HtmlPartial]](mockHttpClient.GET(eqTo(expectedUrl), any, any)(any, any, any))
        .thenReturn(Future.failed(new Exception("ahh")))

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.isEmpty mustEqual true
      }
    }

    "return None when the upstream call returns an unhappy response" in new Setup {
      when(mockConfig.customsSecureMessagingBannerEndpoint).thenReturn(expectedUrl)

      when[Future[HtmlPartial]](mockHttpClient.GET(eqTo(expectedUrl), any, any)(any, any, any))
        .thenReturn(Future.successful(HtmlPartial.Failure(Some(INTERNAL_SERVER_ERROR),"ahh")))

      private val connector = app.injector.instanceOf[SecureMessageConnector]

      running(app) {
        val result = await(connector.getMessageCountBanner(returnTo)(fakeRequest()))
        result.isEmpty mustEqual true
      }
    }

  }

  trait Setup {

    protected val mockHttpClient: HttpClient = mock[HttpClient]
    protected val mockConfig: AppConfig = mock[AppConfig]
    protected val expectedUrl = "messageBannerEndpoint"
    protected val returnTo = "backhere.com"

    protected val app: Application = application()
      .overrides(bind[AppConfig].toInstance(mockConfig))
      .overrides(bind[HttpClient].toInstance(mockHttpClient))
      .build()
  }
}

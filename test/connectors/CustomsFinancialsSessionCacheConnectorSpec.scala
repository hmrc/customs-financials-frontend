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
import domain.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import play.api.{Application, inject}
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId, *}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.SpecBase
import utils.MustMatchers
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CustomsFinancialsSessionCacheConnectorSpec
    extends SpecBase
    with ScalaFutures
    with FutureAwaits
    with DefaultAwaitTimeout
    with MustMatchers {

  "store session" should {
    "save all account links for a session" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when(requestBuilder.withBody(any[AccountLinksRequest]())(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

        when(mockHttpClient.post(any[URL]())(any())).thenReturn(requestBuilder)

        val result = await(connector.storeSession(sessionId.value, someLinks))
        result.status mustBe OK
      }
    }
  }

  "remove session" should {
    "should remove the session from the cache" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

        when(mockHttpClient.delete(any[URL]())(any())).thenReturn(requestBuilder)

        val result = await(connector.removeSession(sessionId.value))
        result.status mustBe OK
      }
    }
  }

  "getAccountNumbers" should {
    "Should return Nothing when no result found" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        val result = await(connector.getAccontLinks(sessionId.value))
        result mustBe None
      }
    }
  }

  "getSessionId" should {
    "Should return Ok and a SessionId when success" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(OK, "Some_String")))

        when(mockHttpClient.get(any())(any())).thenReturn(requestBuilder)

        val result: Option[HttpResponse] = await(connector.getSessionId(sessionId.value))

        result.map { res =>
          {
            res.status mustBe OK
            res.body mustBe "Some_String"
          }
        }
      }
    }

    "Should return 404 when fails to find sessionId" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new RuntimeException(emptyString)))

        when(mockHttpClient.get(any())(any())).thenReturn(requestBuilder)

        val result = await(connector.getSessionId(sessionId.value))
        result mustBe None
      }
    }
  }

  trait Setup {
    val sessionId: SessionId = SessionId(UUID.randomUUID().toString)
    val url = "/some-url"
    val sessionCacheLinks: Seq[SessionCacheAccountLink] = Seq(
      SessionCacheAccountLink(
        "eori1",
        isNiAccount = false,
        "dan1",
        AccountStatusOpen,
        Option(DefermentAccountAvailable),
        "link1"
      ),
      SessionCacheAccountLink(
        "eori2",
        isNiAccount = false,
        "dan2",
        AccountStatusClosed,
        Option(AccountCancelled),
        "link1"
      )
    )

    val someLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId.value,
        "eori1",
        isNiAccount = false,
        "dan1",
        AccountStatusOpen,
        Option(DefermentAccountAvailable),
        "link1",
        LocalDateTime.now
      ),
      AccountLink(
        sessionId.value,
        "eori2",
        isNiAccount = false,
        "dan2",
        AccountStatusClosed,
        Option(AccountCancelled),
        "link1",
        LocalDateTime.now
      )
    )

    val accountLinkRequest = new AccountLinksRequest(sessionId.value, sessionCacheLinks)
    val mockAppConfig: AppConfig = mock[AppConfig]
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val accountLink: AccountLink = AccountLink(
      sessionId.value,
      "eori1",
      isNiAccount = false,
      "1234567",
      AccountStatusOpen,
      Option(DefermentAccountAvailable),
      "link1",
      LocalDateTime.now
    )

    val sessionAccountCacheLink: SessionCacheAccountLink = SessionCacheAccountLink(
      "eori1",
      isNiAccount = false,
      "1234567",
      AccountStatusOpen,
      Option(DefermentAccountAvailable),
      "link1"
    )

    when(mockAppConfig.customsFinancialsSessionCacheUrl).thenReturn(url)

    val app: Application = application()
      .overrides(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[RequestBuilder].toInstance(requestBuilder)
      )
      .build()
  }
}

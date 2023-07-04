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
import domain._
import org.joda.time.DateTime
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, SessionId}
import utils.SpecBase
import java.util.UUID
import scala.concurrent.Future

class CustomsFinancialsSessionCacheConnectorSpec extends SpecBase
  with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  "store session" should {
    "save all account links for a session" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
        val cacheUrl = mockAppConfig.customsFinancialsSessionCacheUrl + "/update-links"
        when[Future[HttpResponse]](mockHttpClient.POST(eqTo(cacheUrl), eqTo(accountLinkRequest), any)(any, any, any, any))
          .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
        val result = await(connector.storeSession(sessionId.value, someLinks))
        result.status mustBe OK
      }
    }
  }

  "remove session" should {
    "should remove the session from the cache" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
        val cacheUrl = mockAppConfig.customsFinancialsSessionCacheUrl + "/remove/" + sessionId.value
        when[Future[HttpResponse]](mockHttpClient.DELETE(eqTo(cacheUrl), any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
        val result = await(connector.removeSession(sessionId.value))
        result.status mustBe OK
      }
    }
  }

  "getAccountNumbers" should {
    "Should return Nothing when no result found" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
        val cacheUrl = mockAppConfig.customsFinancialsSessionCacheUrl +
          "/account-links/" + sessionId.value
        when[Future[HttpResponse]](mockHttpClient.GET(eqTo(cacheUrl),any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
        val result = await(connector.getAccontLinks(sessionId.value))
        result mustBe None
      }
    }
  }

  "getSessionId" should {
    "Should return Ok and a SessionId when success" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when[Future[String]](mockHttpClient.GET(any, any[Seq[(String, String)]],
          any[Seq[(String, String)]])(any, any, any)).thenReturn(Future.successful("Some_String"))

        val result = await(connector.getSessionId(sessionId.value))
        result mustBe Some("Some_String")
      }
    }

    "Should return 404 when fails to find sessionId" in new Setup {
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]

        when[Future[String]](mockHttpClient.GET(any, any[Seq[(String, String)]], any[Seq[(String, String)]])(any, any, any))
          .thenReturn(Future.failed(new RuntimeException("")))

        val result = await(connector.getSessionId(sessionId.value))
        result mustBe None
      }
    }
  }

  trait Setup {
    val sessionId = SessionId(UUID.randomUUID().toString)
    val url = "/some-url"
    val sessionCacheLinks = Seq(
      SessionCacheAccountLink("eori1", false, "dan1", AccountStatusOpen, Option(DefermentAccountAvailable), "link1"),
      SessionCacheAccountLink("eori2", false, "dan2", AccountStatusClosed, Option(AccountCancelled), "link1")
    )

    val someLinks = Seq(
      AccountLink(sessionId.value, "eori1", false, "dan1", AccountStatusOpen, Option(DefermentAccountAvailable), "link1", DateTime.now),
      AccountLink(sessionId.value, "eori2", false, "dan2", AccountStatusClosed, Option(AccountCancelled), "link1", DateTime.now)
    )
    val accountLinkRequest = new AccountLinksRequest(sessionId.value, sessionCacheLinks)
    val mockAppConfig = mock[AppConfig]
    val mockHttpClient = mock[HttpClient]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val accountLink = AccountLink(sessionId.value, "eori1", false, "1234567", AccountStatusOpen, Option(DefermentAccountAvailable), "link1", DateTime.now)
    val sessionAccountCacheLink = SessionCacheAccountLink("eori1", false, "1234567", AccountStatusOpen, Option(DefermentAccountAvailable), "link1")

    when(mockAppConfig.customsFinancialsSessionCacheUrl).thenReturn(url)
    val app = application().overrides(
      inject.bind[AppConfig].toInstance(mockAppConfig),
      inject.bind[HttpClient].toInstance(mockHttpClient)
    ).build()

  }
}


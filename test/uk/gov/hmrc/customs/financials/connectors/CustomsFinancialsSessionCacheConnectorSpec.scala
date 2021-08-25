/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.connectors

import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.{AccountCancelled, AccountLink, AccountStatusClosed, AccountStatusOpen, DefermentAccountAvailable, SessionCacheAccountLink}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException, SessionId}

import java.util.UUID
import scala.concurrent.Future

class CustomsFinancialsSessionCacheConnectorSpec extends SpecBase with ScalaFutures with FutureAwaits with DefaultAwaitTimeout {

  "retrieve session" should {
    "return a Account Link when a valid link id is provided" in new Setup {
      when[Future[SessionCacheAccountLink]](mockHttpClient.GET(eqTo(url + s"/account-link/${sessionId.value}/link1"), any, any)(any, any, any)).thenReturn(Future.successful(sessionAccountCacheLink))
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
        val result: AccountLink = await(connector.retrieveSession(sessionId.value, "link1")).get
        result.eori mustBe accountLink.eori
        result.accountNumber mustBe accountLink.accountNumber
        result.linkId mustBe accountLink.linkId
      }
    }

    "returns None when retrieve link fails" in new Setup {
      when[Future[SessionCacheAccountLink]](mockHttpClient.GET(eqTo(url + s"/account-link/${sessionId.value}/link1"), any, any)(any, any, any)).thenReturn(Future.failed(new InternalServerException("Boom")))
      running(app) {
        val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
        val result = await(connector.retrieveSession(sessionId.value, "link1"))
        result mustBe None
      }
    }
  }

    "store session" should {
      "save  all account links for a session" in new Setup {
        running(app) {
          val connector = app.injector.instanceOf[CustomsFinancialsSessionCacheConnector]
          val cacheUrl = mockAppConfig.customsFinancialsSessionCacheUrl + "/update-links"
          when[Future[HttpResponse]](mockHttpClient.POST(eqTo(cacheUrl), eqTo(accountLinkRequest), any)(any, any, any, any))
            .thenReturn(Future.successful(HttpResponse.apply(Status.OK, "")))
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
          .thenReturn(Future.successful(HttpResponse.apply(Status.OK, "")))
        val result = await(connector.removeSession(sessionId.value))
        result.status mustBe OK
      }
    }
  }

    trait Setup {
      val sessionId = SessionId(UUID.randomUUID().toString)
      val url = "/some-url"
      val sessionCacheLinks = Seq(
        SessionCacheAccountLink("eori1", "dan1", AccountStatusOpen, Option(DefermentAccountAvailable), "link1"),
        SessionCacheAccountLink("eori2", "dan2", AccountStatusClosed, Option(AccountCancelled), "link1")
      )

      val someLinks = Seq(
        AccountLink(sessionId.value, "eori1", "dan1", AccountStatusOpen, Option(DefermentAccountAvailable), "link1", DateTime.now),
        AccountLink(sessionId.value, "eori2", "dan2", AccountStatusClosed, Option(AccountCancelled), "link1", DateTime.now)
      )
      val accountLinkRequest = new AccountLinksRequest(sessionId.value, sessionCacheLinks)
      val mockAppConfig = mock[AppConfig]
      val mockHttpClient = mock[HttpClient]

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val accountLink = AccountLink(sessionId.value, "eori1", "1234567", AccountStatusOpen, Option(DefermentAccountAvailable), "link1", DateTime.now)
      val sessionAccountCacheLink = SessionCacheAccountLink("eori1", "1234567", AccountStatusOpen, Option(DefermentAccountAvailable), "link1")

      when(mockAppConfig.customsFinancialsSessionCacheUrl).thenReturn(url)
      val app = application().overrides(
        bind[AppConfig].toInstance(mockAppConfig),
        bind[HttpClient].toInstance(mockHttpClient)
      ).build()

    }
  }


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

import domain.{EmailUnverifiedResponse, EmailVerifiedResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import play.api.Application
import play.api.inject.bind
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import utils.{MustMatchers, SpecBase}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class CustomsDataStoreConnectorSpec
    extends SpecBase
    with ScalaFutures
    with FutureAwaits
    with DefaultAwaitTimeout
    with MustMatchers {

  "CustomsFinancialApiConnector" should {

    "return verified email" in new Setup {

      running(app) {

        when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(response))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        val result: Future[EmailVerifiedResponse] = customsDataStoreConnector.isEmailVerified(hc)
        await(result) mustBe expectedResult

        verifyEndPoint("http://localhost:9893/customs-data-store/subscriptions/subscriptionsdisplay")
      }
    }

    "return email when calling getEmailAddress" in new Setup {

      running(app) {

        when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(response))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        val result: Future[EmailVerifiedResponse] = customsDataStoreConnector.getEmailAddress(hc)
        await(result) mustBe expectedResult

        verifyEndPoint("http://localhost:9893/customs-data-store/subscriptions/email-display")
      }
    }

    "return unverified email" in new Setup {

      running(app) {

        when(requestBuilder.execute(any[HttpReads[EmailUnverifiedResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(EmailUnverifiedResponse(Some("unverified@email.com"))))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        val result: Future[Option[String]] = customsDataStoreConnector.isEmailUnverified(hc)
        assert(await(result).contains("unverified@email.com"))

        verifyEndPoint("http://localhost:9893/customs-data-store/subscriptions/unverified-email-display")
      }
    }

    "return None when email is not found" in new Setup {

      running(app) {

        when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(EmailVerifiedResponse(None)))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        val result: Future[EmailVerifiedResponse] = customsDataStoreConnector.isEmailVerified(hc)
        await(result) mustBe EmailVerifiedResponse(None)
      }
    }
  }

  trait Setup {

    val expectedResult: EmailVerifiedResponse = EmailVerifiedResponse(Some("verifiedEmail"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]

    val response: EmailVerifiedResponse = EmailVerifiedResponse(Some("verifiedEmail"))

    when(requestBuilder.execute(any[HttpReads[EmailVerifiedResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(response))

    when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
    when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
      .thenReturn(Future.successful(HttpResponse(OK, emptyString)))

    when(mockHttpClient.delete(any[URL]())(any())).thenReturn(requestBuilder)

    val app: Application = application()
      .overrides(
        bind[HttpClientV2].toInstance(mockHttpClient),
        bind[RequestBuilder].toInstance(requestBuilder)
      )
      .build()

    val customsDataStoreConnector: CustomsDataStoreConnector =
      app.injector.instanceOf[CustomsDataStoreConnector]

    protected def verifyEndPoint(expectedEndPoint: String): Unit = {
      val urlCaptor = ArgumentCaptor.forClass(classOf[URL])
      verify(mockHttpClient).get(urlCaptor.capture)(any())
      assert(urlCaptor.getValue.toString == expectedEndPoint)
    }
  }
}

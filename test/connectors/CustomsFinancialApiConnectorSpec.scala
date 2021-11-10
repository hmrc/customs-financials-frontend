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

package connectors

import domain.EmailVerifiedResponse
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject.bind
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import utils.SpecBase
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

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
  }

  trait Setup {

    val expectedResult = EmailVerifiedResponse(Some("verifiedEmail"))
    implicit val hc: HeaderCarrier = HeaderCarrier()
    private val mockHttpClient = mock[HttpClient]

    val response = EmailVerifiedResponse(Some("verifiedEmail"))

    when[Future[EmailVerifiedResponse]](mockHttpClient.GET(any, any, any)(any, any, any))
      .thenReturn(Future.successful(response))

    val app = application().overrides(
      bind[HttpClient].toInstance(mockHttpClient)
    ).build()


  }
}

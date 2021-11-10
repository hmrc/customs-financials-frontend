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

package uk.gov.hmrc.customs.financials.actionbuilders

import actionbuilders.{AuthenticatedRequest, EmailAction}
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Email
domain.{UndeliverableEmail, UnverifiedEmail}
services.DataStoreService
utils.SpecBase
import uk.gov.hmrc.http.ServiceUnavailableException

import scala.concurrent.Future

class EmailActionSpec extends SpecBase {

  "EmailAction" should {
    "Let requests with validated email through" in new Setup {
      running (app) {
        when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
        val response = await(emailAction.filter(authenticatedRequest))
        response mustBe None
      }
    }

    "Display undeliverable page when getEmail returns undeliverable" in new Setup {
      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Left(UndeliverableEmail("some@email.com"))))
      val response = await(emailAction.filter(authenticatedRequest)).value
      response.header.status mustBe OK
      val result = contentAsString(Future.successful(response))

      val html = Jsoup.parse(result)
      html.getElementsByTag("h1").text mustBe s"There's a problem with your email address"
    }

    "Let request through, when getEmail throws service unavailable exception" in new Setup {
      running(app){
        when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.failed(new ServiceUnavailableException("")))
        val response = await(emailAction.filter(authenticatedRequest))
        response mustBe None
      }
    }

    "Redirect users with unvalidated emails" in new Setup {
      running(app) {
        when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))
        val response = await(emailAction.filter(authenticatedRequest))
        response.get.header.status mustBe Status.SEE_OTHER
        response.get.header.headers(LOCATION) must include("/verify-your-email")
      }
    }
  }

  trait Setup {
    val mockDataStoreService: DataStoreService = mock[DataStoreService]

    val app = application().overrides(
      inject.bind[DataStoreService].toInstance(mockDataStoreService)
    ).build()

    val emailAction = app.injector.instanceOf[EmailAction]

    val authenticatedRequest = AuthenticatedRequest(FakeRequest("GET","/"), newUser())
  }
}

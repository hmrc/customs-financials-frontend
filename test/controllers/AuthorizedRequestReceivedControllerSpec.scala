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

package controllers

import domain.{RequestAuthoritiesCSVError, RequestAuthoritiesCsvResponse, UnverifiedEmail}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchersSugar.any
import play.api.http.Status.OK
import play.api.inject
import play.api.test.Helpers.{GET, contentAsString, defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsEmpty}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import utils.SpecBase
import scala.concurrent.Future

class AuthorizedRequestReceivedControllerSpec extends SpecBase {

  "requestAuthoritiesCsv" should {
    "should be sent to authorisedToViewRequestReceived when request successful" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        html.text().contains("Request received") shouldBe true
      }
    }

    "should be directed to InternalServerError page when no email is returned from data-store" in new Setup {
      when(mockDataStoreService.getEmail(any)(any))
        .thenReturn(Future.successful(Left(UnverifiedEmail)))
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe 500
        html.text().contains("Sorry, we’re experiencing technical difficulties") shouldBe true
        html.text().contains("Please try again in a few minutes.") shouldBe true
      }
    }

    "should be directed to InternalServerError page when error is returned from ACC41" in new Setup {
      when(mockApiService.requestAuthoritiesCsv(any, any)(any))
        .thenReturn(Future.successful(Left(RequestAuthoritiesCSVError)))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe 500
        html.text().contains("Sorry, we’re experiencing technical difficulties") shouldBe true
        html.text().contains("Please try again in a few minutes.") shouldBe true
      }
    }
  }

  trait Setup {
    val mockApiService: ApiService = mock[ApiService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]
    val requestAuthorityCsvResponse: RequestAuthoritiesCsvResponse = RequestAuthoritiesCsvResponse("date")

    when(mockApiService.requestAuthoritiesCsv(any, any)(any))
      .thenReturn(Future.successful(Right(requestAuthorityCsvResponse)))

    when(mockDataStoreService.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("address@email.com"))))

    val app = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).configure("features.new-agent-view-enabled" -> false).build()

    val controller = app.injector.instanceOf[AuthorizedRequestReceivedController]
  }
}

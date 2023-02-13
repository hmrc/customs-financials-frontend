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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.test.Helpers._
import play.api.test.Helpers.baseApplicationBuilder.overrides
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import utils.SpecBase

import scala.concurrent.Future

class EmailControllerSpec extends SpecBase {

  "The Verify Your Email page" should {
    "return OK if signed in and have a link to verify email address" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.EmailController.showUnverified.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) mustBe OK
        html.containsLinkWithText("/manage-email-cds/service/customs-finance", "Verify or change email address") mustBe true
      }
    }
  }

  trait Setup {
    val mockApiService: ApiService = mock[ApiService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]

    when(mockDataStoreService.getEmail(any)(any))
      .thenReturn(Future.successful(Right(Email("address@email.com"))))

    val app = application()
    .overrides(
      inject.bind[DataStoreService].toInstance(mockDataStoreService)
    ).configure("features.new-agent-view-enabled" -> false).build()
  }
}

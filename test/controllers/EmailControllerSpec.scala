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

package controllers

import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers._
import utils.SpecBase


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
    val app = application().build()
  }

}

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

import play.api.test.Helpers._
import utils.SpecBase
import utils.MustMatchers

class StaticPagesControllerSpec extends SpecBase with MustMatchers {

  "accessibility page" should {
    "return OK / 303" in {
      val app = application().build()

      running(app) {
        val request = fakeRequest(GET, routes.StaticPagesController.accessibility.url)
        val result  = route(app, request).value

        status(result) mustBe SEE_OTHER
      }
    }
  }
}

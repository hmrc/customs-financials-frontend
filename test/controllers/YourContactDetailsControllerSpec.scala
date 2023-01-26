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

import connectors.SdesConnector
import domain._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.any
import play.api.test.Helpers._
import play.api.{Application, inject}
import services.{ApiService, DataStoreService}
import utils.SpecBase
import scala.concurrent.Future
import scala.reflect.io.File
import views.html.your_contact_details.your_contact_details

class YourContactDetailsControllerSpec extends SpecBase {

  "Your Contact Details View Page" should {

    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.YourContactDetailsController.onPageLoad().url)
        val result = route(app, request).value
        status(result) should be(OK)
      }
    }
  }

  trait Setup {
    val app = application().build()
  }
}

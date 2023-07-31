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

package views.components

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.SpecBase
import views.html.components.authorities_notification_panel

class AuthoritiesNotificationPanelSpec extends SpecBase {

  "AuthoritiesNotificationPanel view" should {
    "display the correct guidance when only GB authority file exists" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorities_notification_panel].apply(
          Option("gbURL"),
          None,
          date,
          fileExists = true
        ).body)

      val elements: Elements = view.getElementsByClass("govuk-body")
      elements.size() mustBe 2

      elements.get(0).getElementsByTag("p").html() must contain
      messages(app)("cf.authorities.notification-panel.p1")

      elements.get(1).html() mustBe messages(app)("cf.authorities.notification-panel.p2", date)

      view.getElementById("gb-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.gb-authority")
      view.getElementById("gb-csv-authority-link").attr("href") mustBe "gbURL"
    }

    "display the correct guidance when both GB and XI authorities' file exist" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorities_notification_panel].apply(
          Option("gbURL"),
          Option("xiURL"),
          date,
          fileExists = true
        ).body)

      val elements: Elements = view.getElementsByClass("govuk-body")
      elements.size() mustBe 2

      view.getElementById("gb-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.gb-authority")
      view.getElementById("gb-csv-authority-link").attr("href") mustBe "gbURL"

      view.getElementById("xi-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.xi-authority")
      view.getElementById("xi-csv-authority-link").attr("href") mustBe "xiURL"

      elements.get(0).getElementsByTag("p").html() must contain
      messages(app)("cf.authorities.notification-panel.p1")

      elements.get(1).html() mustBe messages(app)("cf.authorities.notification-panel.p2", date)
    }

    "not display the guidance when file does not exists" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorities_notification_panel].apply(
          None, None, date, fileExists = false).body)

      Option(view.getElementById("notification-panel")) mustBe None

      view.getElementsByClass("govuk-body").size() mustBe 0
    }
  }

  trait Setup {
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val date = "19th August 2022"
  }
}

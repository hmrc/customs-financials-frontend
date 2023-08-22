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

package views.authorised_to_view

import config.AppConfig
import forms.EoriNumberFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.SpecBase
import views.html.authorised_to_view.authorised_to_view_search

class AuthorisedToViewSearchSpec extends SpecBase {
  "AuthorisedToViewSearch view" should {
    "display correct title and guidance" in new SetUp {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search].apply(
          form,
          Option("url"),
          None,
          date,
          fileExists = true,
          isXiEoriEnabled = false
        ).body)

      view.title() mustBe "Find accounts you have authority to use - View your customs financial accounts " +
        "- GOV.UK - View your customs financial accounts - GOV.UK"
      val elements: Elements = view.getElementsByClass("govuk-list govuk-list--bullet")

      val liElements: Elements = elements.get(0).getElementsByTag("li")

      liElements.get(0).html() mustBe messages(app)("cf.search.authorities.eori")
      liElements.get(1).html() mustBe messages(app)("cf.search.authorities.account")
    }

    "display correct title and XI Eori guidance" in new SetUp {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search].apply(
          form,
          Option("url"),
          None,
          date,
          fileExists = true,
          isXiEoriEnabled = true
        ).body)

      view.title() mustBe "Find accounts you have authority to use - View your customs financial accounts " +
        "- GOV.UK - View your customs financial accounts - GOV.UK"
      val elements: Elements = view.getElementsByClass("govuk-list govuk-list--bullet")

      val liElements: Elements = elements.get(0).getElementsByTag("li")

      liElements.get(0).html() mustBe messages(app)("cf.search.authorities.eori.xi")
      liElements.get(1).html() mustBe messages(app)("cf.search.authorities.account")
    }

    "display correct link for CSV file" in new SetUp {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search].apply(
          form,
          Option("url"),
          None,
          date,
          fileExists = true,
          isXiEoriEnabled = true
        ).body)

      view.getElementById("authorised-request-csv-link").html() mustBe
        messages(app)("cf.search.authorities.link")

      val anchorTag: Elements = view.getElementById("authorised-request-csv-link").getElementsByTag("a")

      anchorTag.attr("href") mustBe
        controllers.routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url
    }

    "display correct guidance when csv url is of GB authority" in new SetUp {
      val gbAuthUrl = "gbURL"

      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search].apply(
          form,
          Option(gbAuthUrl),
          None,
          date,
          fileExists = true,
          isXiEoriEnabled = true
        ).body)

      view.getElementById("authorised-request-csv-link").html() mustBe
        messages(app)("cf.search.authorities.link")

      val anchorTag: Elements = view.getElementById("authorised-request-csv-link").getElementsByTag("a")

      anchorTag.attr("href") mustBe
        controllers.routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url

      view.getElementById("gb-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.gb-authority")
      view.getElementById("gb-csv-authority-link").attr("href") mustBe gbAuthUrl
    }

    "display correct guidance when csv urls for both GB and XI authorities are available" in new SetUp {
      val gbAuthUrl = "gbURL"
      val xiAuthUrl = "xiURL"

      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search].apply(
          form,
          Option(gbAuthUrl),
          Option(xiAuthUrl),
          date,
          fileExists = true,
          isXiEoriEnabled = true
        ).body)

      view.getElementById("authorised-request-csv-link").html() mustBe
        messages(app)("cf.search.authorities.link")

      val anchorTag: Elements = view.getElementById("authorised-request-csv-link").getElementsByTag("a")

      anchorTag.attr("href") mustBe
        controllers.routes.AuthorizedRequestReceivedController.requestAuthoritiesCsv().url

      view.getElementById("gb-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.gb-authority")
      view.getElementById("gb-csv-authority-link").attr("href") mustBe gbAuthUrl

      view.getElementById("xi-csv-authority-link").html() mustBe
        messages(app)("cf.authorities.notification-panel.a.xi-authority")
      view.getElementById("xi-csv-authority-link").attr("href") mustBe xiAuthUrl
    }
  }

  trait SetUp {
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val form: Form[String] = new EoriNumberFormProvider().apply()
    val date = "SomeDate"
  }
}

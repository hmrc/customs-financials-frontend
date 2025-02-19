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

package views

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.MustMatchers
import views.html.Layout
import utils.SpecBase

class LayoutSpec extends SpecBase with MustMatchers {

  "layout" should {

    "display correct guidance" when {

      "title, browserBackLink, and back link are provided" in new Setup {
        val titleMsg        = "test_title"
        val browserBackUrl  = "browser-test.com"
        val backLinkUrl     = "test.com"

        val layoutView: Document = Jsoup.parse(
          app.injector
            .instanceOf[Layout]
            .apply(
              pageTitle = Some(titleMsg),
              backLink = Some(backLinkUrl),
              browserBackLink = Some(browserBackUrl)
            )(content)
            .body
        )

        shouldContainCorrectTitle(layoutView, titleMsg)
        shouldContainCorrectServiceUrls(layoutView)
        shouldContainCorrectBackLink(layoutView, Some(browserBackUrl), browserBackLink = true)
        shouldContainCorrectBanners(layoutView)
      }

      "only back link is provided (no browserBackLink)" in new Setup {
        val titleMsg = "test_title"
        val backLinkUrl  = "test.com"

        val layoutView: Document = Jsoup.parse(
          app.injector
            .instanceOf[Layout]
            .apply(
              pageTitle = Some(titleMsg),
              backLink = Some(backLinkUrl),
              browserBackLink = None
            )(content)
            .body
        )

        shouldContainCorrectTitle(layoutView, titleMsg)
        shouldContainCorrectServiceUrls(layoutView)
        shouldContainCorrectBackLink(layoutView, Some(backLinkUrl), browserBackLink = false)
        shouldContainCorrectBanners(layoutView)
      }

      "there is no value for title, browserBackLink, or back link" in new Setup {
        val layoutView: Document = Jsoup.parse(app.injector.instanceOf[Layout].apply()(content).body)

        shouldContainCorrectTitle(layoutView)
        shouldContainCorrectServiceUrls(layoutView)
        shouldContainCorrectBackLink(layoutView, None)
        shouldContainCorrectBanners(layoutView)
      }
    }
  }

  private def shouldContainCorrectTitle(viewDoc: Document, title: String = emptyString)(implicit msgs: Messages) =
    if (title.isEmpty) {
      viewDoc.title() mustBe s"${msgs("service.name")} - GOV.UK"
    } else {
      viewDoc.title() mustBe s"$title - ${msgs("service.name")} - GOV.UK"
    }

  private def shouldContainCorrectServiceUrls(viewDoc: Document) = {
    viewDoc.html().contains(controllers.routes.CustomsFinancialsHomeController.index.url) mustBe true
    viewDoc.html().contains(controllers.routes.LogoutController.logout.url) mustBe true
    viewDoc.html().contains("/accessibility-statement/customs-financials") mustBe true
  }

  private def shouldContainCorrectBackLink(viewDoc: Document, backLinkUrl: Option[String] = None, browserBackLink: Boolean = false) = {
    if (backLinkUrl.isDefined) {
      val backLinkElement = viewDoc.getElementsByClass("govuk-back-link")

      backLinkElement.text() mustBe "Back"
      backLinkElement.attr("href") mustBe backLinkUrl.get

      if (browserBackLink) {
        backLinkElement.attr("id") mustBe "browser-back-link"
      }
    } else {
      viewDoc.getElementsByClass("govuk-back-link").size() mustBe 0
    }
  }

  private def shouldContainCorrectBanners(viewDoc: Document) = {
    viewDoc
      .getElementsByClass("govuk-phase-banner")
      .text() mustBe "BETA This is a new service – your feedback will help us to improve it."

    viewDoc
      .getElementsByClass("hmrc-user-research-banner")
      .text() mustBe "Help make GOV.UK better Sign up to take part in research (opens in new tab)" +
      " Hide message Hide message. I do not want to take part in research"
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages                               = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = fakeRequest("GET", "test_path")
    implicit val appConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

    val content: Html = Html("test")
  }
}

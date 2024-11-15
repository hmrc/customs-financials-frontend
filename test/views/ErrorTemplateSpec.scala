/*
 * Copyright 2024 HM Revenue & Customs
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
import org.jsoup.nodes.{Document, Element}
import play.api.Application
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import utils.{SpecBase, MustMatchers}
import views.html.error_states.error_template

class ErrorTemplateSpec extends SpecBase with MustMatchers {

  "ErrorTemplateSpec view" should {

    "display correct information" when {

      "main header is correct" in new Setup {
        running(app) {
          viewWithBackLink.getElementsByTag("h1").text() mustBe messages(
            "cf.error.technicalDifficulties.heading"
          )
        }
      }

      "backlink should be visible" in new Setup {
        running(app) {
          backLinkWithLink.isDefined mustBe true
        }
      }

      "backlink URL is correct" in new Setup {
        running(app) {
          backLinkWithLink.map(_.attr("href")) mustBe Some(expectedBackLinkPath)
        }
      }

      "error message is correct" in new Setup {
        running(app) {
          viewWithBackLink.getElementsByClass("govuk-body").get(0).text() mustBe messages(
            "cf.error.technicalDifficulties.message"
          )
        }
      }
    }

    "not display backlink" when {
      "backlink is not passed as a parameter" in new Setup {
        running(app) {
          backLinkWithoutLink.isEmpty mustBe true
        }
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    implicit val messages: Messages = messagesApi.preferred(request)

    val expectedBackLinkPath = "/customs/payment-records/authorized-to-view"

    val viewWithBackLink: Document = Jsoup.parse(
      app.injector.instanceOf[error_template].apply(
        pageTitle = messages("cf.error.technicalDifficulties.title"),
        heading = messages("cf.error.technicalDifficulties.heading"),
        backLink = Some(expectedBackLinkPath),
        details = Seq(messages("cf.error.technicalDifficulties.message")): _*
      ).body
    )

    val viewWithoutBackLink: Document = Jsoup.parse(
      app.injector.instanceOf[error_template].apply(
        pageTitle = messages("cf.error.technicalDifficulties.title"),
        heading = messages("cf.error.technicalDifficulties.heading"),
        backLink = None,
        details = Seq(messages("cf.error.technicalDifficulties.message")): _*
      ).body
    )

    val backLinkWithLink: Option[Element] =
      Option(viewWithBackLink.select(".govuk-back-link").first())

    val backLinkWithoutLink: Option[Element] =
      Option(viewWithoutBackLink.select(".govuk-back-link").first())

  }
}

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

import forms.EoriNumberFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.SpecBase
import views.html.components.inputText
import utils.MustMatchers

class InputTextSpec extends SpecBase with MustMatchers {

  "InputText" should {

    "display the correct view" when {

      "EU Eori flag is not enabled" in new Setup {
        running(app) {
          val view: Document = Jsoup.parse(
            app.injector
              .instanceOf[inputText]
              .apply(
                form = validForm,
                id = id,
                name = name,
                label = labelMsgKey,
                isPageHeading = false,
                hint = None
              )
              .body
          )

          view.getElementsByTag("label").html() mustBe msgs(labelMsgKey)
          view.getElementById("value").`val`() mustBe "GB123456789012"

          intercept[RuntimeException] {
            view.getElementById("value-hint").html()
          }
        }
      }

      "EU Eori flag is enabled" in new Setup {
        running(app) {
          val view: Document = Jsoup.parse(
            app.injector
              .instanceOf[inputText]
              .apply(
                form = validFormWithEUEoriFlagEnabled,
                id = id,
                name = name,
                label = labelMsgKey,
                isPageHeading = false,
                hint = None
              )
              .body
          )

          view.getElementsByTag("label").html() mustBe msgs(labelMsgKey)
          view.getElementById("value").`val`() mustBe euEori

          intercept[RuntimeException] {
            view.getElementById("value-hint").html()
          }
        }
      }

    }

    "display the correct hint" when {

      "EU Eori flag is not enabled" in new Setup {
        val view: Document = Jsoup.parse(
          app.injector
            .instanceOf[inputText]
            .apply(
              form = validForm,
              id = id,
              name = name,
              label = labelMsgKey,
              isPageHeading = false,
              hint = Some(hintText)
            )
            .body
        )

        view.getElementById("value-hint").html() mustBe hintText
      }

      "EU Eori flag is enabled" in new Setup {
        val view: Document = Jsoup.parse(
          app.injector
            .instanceOf[inputText]
            .apply(
              form = validFormWithEUEoriFlagEnabled,
              id = id,
              name = name,
              label = labelMsgKey,
              isPageHeading = false,
              hint = Some(hintText)
            )
            .body
        )

        view.getElementById("value-hint").html() mustBe hintText
      }
    }

    "display error if form has any error" when {

      "EU Eori flag is not enabled" in new Setup {
        running(app) {
          val view: Document = Jsoup.parse(
            app.injector
              .instanceOf[inputText]
              .apply(
                form = invalidForm,
                id = id,
                name = name,
                label = labelMsgKey,
                isPageHeading = false,
                hint = None
              )
              .body
          )

          view.getElementById("value-error").childNodes().size() must be > 0
          view.getElementsByClass("govuk-visually-hidden").html() mustBe "Error:"
        }
      }

      "EU Eori flag is enabled" in new Setup {
        running(app) {
          val view: Document = Jsoup.parse(
            app.injector
              .instanceOf[inputText]
              .apply(
                form = invalidFormWithEUEoriFlagEnabled,
                id = id,
                name = name,
                label = labelMsgKey,
                isPageHeading = false,
                hint = None
              )
              .body
          )

          view.getElementById("value-error").childNodes().size() must be > 0
          view.getElementsByClass("govuk-visually-hidden").html() mustBe "Error:"
        }
      }
    }

    "display label with govuk-label--xl class" when {

      "EU Eori flag is not enabled" in new Setup {
        val view: Document = Jsoup.parse(
          app.injector
            .instanceOf[inputText]
            .apply(
              form = validForm,
              id = id,
              name = name,
              label = labelMsgKey,
              isPageHeading = true,
              hint = None
            )
            .body
        )

        view.getElementsByClass("govuk-label--xl").text() mustBe msgs(labelMsgKey)
        view.getElementsByTag("label").html() mustBe msgs(labelMsgKey)
        view.getElementById("value").`val`() mustBe "GB123456789012"
      }

      "EU Eori flag is enabled" in new Setup {
        val view: Document = Jsoup.parse(
          app.injector
            .instanceOf[inputText]
            .apply(
              form = validFormWithEUEoriFlagEnabled,
              id = id,
              name = name,
              label = labelMsgKey,
              isPageHeading = true,
              hint = None
            )
            .body
        )

        view.getElementsByClass("govuk-label--xl").text() mustBe msgs(labelMsgKey)
        view.getElementsByTag("label").html() mustBe msgs(labelMsgKey)
        view.getElementById("value").`val`() mustBe euEori
      }
    }

  }

  trait Setup {
    val app: Application        = application().build()
    implicit val msgs: Messages = messages(app)

    val labelMsgKey = "cf.search.authorities"
    val id          = "value"
    val name        = "value"
    val euEori      = "FR123456789012"

    val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> "GB123456789012"))

    val validFormWithEUEoriFlagEnabled: Form[String] =
      new EoriNumberFormProvider().apply().bind(Map("value" -> euEori))

    val invalidForm: Form[String]                      = new EoriNumberFormProvider().apply().bind(Map("value" -> "ABC"))
    val invalidFormWithEUEoriFlagEnabled: Form[String] =
      new EoriNumberFormProvider().apply(isEUEoriEnabled = true).bind(Map("value" -> "FR"))

    val hintText = "hint text"
  }
}

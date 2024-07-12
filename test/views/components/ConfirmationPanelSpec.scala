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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.confirmation_panel

class ConfirmationPanelSpec extends SpecBase {

  "view" should {

    "display correct contents" when {
      "body and id have some value" in new Setup {
        val view: Document = viewDoc(h1, Some(body), Some(id))

        view.getElementsByClass("govuk-panel__title").text() mustBe msgs(h1)
        view.getElementsByClass("govuk-panel__body").text() mustBe msgs(body)
      }

      "body and id have None" in new Setup {
        val view: Document = viewDoc(h1)

        view.getElementsByClass("govuk-panel--confirmation").contains(msgs(h1)) mustBe false
        view.getElementsByClass("govuk-panel--confirmation").contains(msgs(body)) mustBe false
      }

      "body has some value and id have None" in new Setup {
        val view: Document = viewDoc(h1, Some(body))

        view.getElementsByClass("govuk-panel__title").text() mustBe msgs(h1)
        view.getElementsByClass("govuk-panel__body").text() mustBe msgs(body)
      }

      "body has None and id has some value" in new Setup {
        val view: Document = viewDoc(h1, id = Some(id))

        view.getElementsByClass("govuk-panel__title").text() mustBe msgs(h1)
        view.getElementsByClass("govuk-panel--confirmation").contains(msgs(body)) mustBe false
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)

    val body = "cf.not-subscribed-to-cds.detail.get-access-info"
    val h1 = "cf.account.authorized-to-view.title"
    val id = "test_id"

    def viewDoc(h1: String,
                body: Option[String] = None,
                id: Option[String] = None): Document =
      Jsoup.parse(app.injector.instanceOf[confirmation_panel].apply(
        h1,
        body,
        id
      ).body)
  }
}

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
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.authorised_account_search_inset

class AuthorisedAccountSearchInsetSpec extends SpecBase {

  "view" should {
    "display correct contents" when {
      "firstLine and secondLine have some value" in new Setup {
        val view: Document = viewDoc(firstLineMsg, Some(secondLineMsg))

        view.text().contains(msgs(firstLineMsg)) mustBe true
        view.text().contains(msgs(secondLineMsg)) mustBe true
      }

      "secondLine has None" in new Setup {
        val view: Document = viewDoc(firstLineMsg)

        view.text().contains(msgs(firstLineMsg)) mustBe true
        view.text().contains(msgs(secondLineMsg)) mustBe false
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)

    val firstLineMsg = "cf.not-subscribed-to-cds.detail.subscribe-cds.link"
    val secondLineMsg = "cf.customs-financials-home.notification.adjustments"

    def viewDoc(firstLine: String,
                secondLine: Option[String] = None): Document =
      Jsoup.parse(app.injector.instanceOf[authorised_account_search_inset].apply(firstLine, secondLine).body)
  }

}

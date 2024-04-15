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
import utils.SpecBase
import views.html.components.newTabLink

class NewTabLinkSpec extends SpecBase {

  "view" should {

    "display correct contents" when {
      "preLinkMessage is available" in new Setup {
        viewDoc.text().contains(msgs(preLinkMessage)) mustBe true

        val elementByClass: Elements = viewDoc.getElementsByClass("govuk-link")
        elementByClass.get(0).text() mustBe linkMessage
      }

      "preLinkMessage in None" in new Setup {
        viewDocWithNoPreLinkMessage.text().contains(msgs(preLinkMessage)) mustBe false

        val elementByClass: Elements = viewDoc.getElementsByClass("govuk-link")
        elementByClass.get(0).text() mustBe linkMessage
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val linkMessage = "test_link_message"
    val href: String = "test_link"
    val preLinkMessage = "cf.not-subscribed-to-cds.detail.subscribe-cds.link"

    val viewDoc: Document =
      Jsoup.parse(app.injector.instanceOf[newTabLink].apply(linkMessage, href, Some(preLinkMessage)).body)

    val viewDocWithNoPreLinkMessage: Document =
      Jsoup.parse(app.injector.instanceOf[newTabLink].apply(linkMessage, href).body)
  }
}

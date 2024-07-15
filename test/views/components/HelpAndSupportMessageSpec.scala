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

import play.api.i18n.Messages
import play.api.test.Helpers
import utils.SpecBase
import org.scalatest.matchers.must.{Matchers => MustMatchers}

class HelpAndSupportMessageSpec extends SpecBase with MustMatchers {

  implicit val messages: Messages = Helpers.stubMessages()

  "Help And Support card" should {

    "display a help and support message with heading class as non-default" in {
      val page = Jsoup.parse(views.html.components.help_and_support_message("govuk-heading-s").body)

      page.getElementById("help_and_support")
        .text mustBe "cf.help-and-support.title cf.account.help-support.message" +
        " cf.account.help-support.numbers. cf.account.help-support.office-hours"

      page.getElementsByClass("govuk-heading-s").isEmpty mustBe false
      page.getElementById("contact-number")
        .text mustBe "cf.account.help-support.message cf.account.help-support.numbers."
      page.getElementById("office-hours").text mustBe "cf.account.help-support.office-hours"
    }

    "display help and support message for default case" in {
      val page = Jsoup.parse(views.html.components.help_and_support_message().body)

      page.getElementById("help_and_support")
      page.getElementsByClass("govuk-heading-m").isEmpty mustBe false
      page.getElementById("contact-number")
        .text mustBe "cf.account.help-support.message cf.account.help-support.numbers."
      page.getElementById("office-hours").text mustBe "cf.account.help-support.office-hours"
    }
  }
}

/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.views.components

import org.jsoup.Jsoup
import play.api.test.Helpers
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.views

class HelpAndSupportMessageSpec extends SpecBase {

  implicit val messages = Helpers.stubMessages()

  "Help And Support card" should {

    "display a help and support message with heading class as non-default" in {
      val page = Jsoup.parse(views.html.components.help_and_support_message("govuk-heading-s", help = "message", contactNumber= "numbers", officeHours ="office-hours").body)
      page.getElementById("help_and_support").text mustBe "cf.help-and-support.title message numbers. office-hours"
      page.getElementsByClass("govuk-heading-s").isEmpty mustBe false
      page.getElementById("contact-number").text mustBe "message numbers."
      page.getElementById("office-hours").text mustBe "office-hours"
    }

    "display help and support message for default case" in {
      val page = Jsoup.parse(views.html.components.help_and_support_message().body)
      page.getElementById("help_and_support")
      page.getElementsByClass("govuk-heading-m").isEmpty mustBe false
      page.getElementById("contact-number").text mustBe "cf.account.help-support.message cf.account.help-support.numbers."
      page.getElementById("office-hours").text mustBe "cf.account.help-support.office-hours"
    }
  }
}

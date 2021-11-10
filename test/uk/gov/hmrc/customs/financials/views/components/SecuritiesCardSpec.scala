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
utils.SpecBase
views

class SecuritiesCardSpec extends SpecBase {

  "Securities Card" should {

    implicit val messages = Helpers.stubMessages()

    "be marked with class 'securities-statements'" in {
      val content = Jsoup.parse(views.html.components.securities_card().body)
      content.getElementsByClass("securities-statements").isEmpty mustBe false
    }

    "include a description" in {
      val content = Jsoup.parse(views.html.components.securities_card().body)
      content.getElementsContainingText("cf.securities.view-statements.description").isEmpty mustBe false
    }

    "include link to the security statements page" in {
      val content = Jsoup.parse(views.html.components.securities_card().body)
      content.getElementsByTag("a").text mustBe "cf.customs-financials-home.securities.link"
      content.getElementsByTag("a").attr("href") mustBe "/customs/payment-records/adjustments"
    }
  }

}

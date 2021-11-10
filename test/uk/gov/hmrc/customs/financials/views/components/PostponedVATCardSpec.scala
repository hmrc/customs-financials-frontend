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

class PostponedVATCardSpec extends SpecBase {

  implicit val messages = Helpers.stubMessages()

  "Postponed VAT Card" should {

    "be marked with class 'postponed-vat-statements'" in {
      val content = Jsoup.parse(views.html.components.postponed_vat_card().body)
      content.getElementsByClass("postponed-vat-statements").isEmpty mustBe false
    }

    "work when there are certificates available and" should {

      "include a description" in {
        val content = Jsoup.parse(views.html.components.postponed_vat_card().body)
        content.getElementsContainingText("cf.postponed-vat.view-certificates.description").isEmpty mustBe false
      }

      "include link to the postponed vat page" in {
        val content = Jsoup.parse(views.html.components.postponed_vat_card().body)
        content.getElementsContainingText("cf.customs-financials-home.pvat.link").isEmpty mustBe false
        content.containsLink("/customs/payment-records/postponed-vat")
      }
    }

  }

}

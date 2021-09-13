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
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.views.html.components.duty_deferment_inaccurate_balances_message

class DutyDefermentInaccurateBalancesSpec extends SpecBase {

  "Duty deferment inaccurate balances warning message" should {

    "have an id 'duty-deferment-balances-warning'" in new Setup {
      running(app) {
        view.containsElementById("duty-deferment-balances-warning")
      }
    }

    "display a warning icon" in new Setup {
      running(app) {
        view.getElementById("duty-deferment-balances-warning").getElementsByClass("govuk-warning-text__icon").text mustBe "!"
      }
    }

    "include link to duty deferment scheme contact" in new Setup {
      running(app) {
        view.getElementById("duty-deferment-balances-warning").getElementsByTag("a").text mustBe "cf.duty-deferment.inaccurateBalance.contactLink"
        view.getElementById("duty-deferment-balances-warning").getElementsByTag("a").attr("href") mustBe appConfig.dutyDefermentSchemeContactLink
      }
    }
  }

  trait Setup {
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    implicit val messages = Helpers.stubMessages()

    val view = Jsoup.parse(app.injector.instanceOf[duty_deferment_inaccurate_balances_message].apply().body)
  }

}
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
import play.api.Application
import play.api.i18n.Messages
import play.api.test.Helpers
import play.api.test.Helpers.running
import utils.SpecBase
import views.html.account_cards.duty_deferment_inaccurate_balances_message

class DutyDefermentInaccurateBalancesSpec extends SpecBase {

  "Duty deferment inaccurate balances warning message" should {
    "have an id 'duty-deferment-balances-warning'" in new Setup {
      running(app) {
        view.containsElementById("duty-deferment-balances-warning")
      }
    }
  }

  trait Setup {
    val app: Application              = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val messages: Messages   = Helpers.stubMessages()
    val view: Document                = Jsoup.parse(app.injector.instanceOf[duty_deferment_inaccurate_balances_message].apply().body)
  }
}

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

package views.error_states

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.SpecBase
import views.html.error_states.account_not_available

class AccountNotAvailableSpec extends SpecBase {

  "AccountNotAvailable view" should {
    "render correctly" in new Setup {
      view.text().contains(messages(app)("cf.customs-financials-home.partial.heading")) mustBe true
      view.text().contains(messages(app)("cf.customs-financials-home.partial.text")) mustBe true
      view.text().contains(messages(app)("cf.customs-financials-home.import-vat.title")) mustBe true
      view.text().contains(messages(app)("cf.customs-financials-home.pvat.title")) mustBe true
      view.text().contains(messages(app)("cf.customs-financials-home.securities.title")) mustBe true
      view.text().contains(messages(app)("cf.import-vat.view-certificates.description")) mustBe true
      view.text().contains(messages(app)("cf.postponed-vat.view-certificates.description")) mustBe true
      view.text().contains(messages(app)("cf.securities.view-statements.description")) mustBe true
    }
  }

  trait Setup {
    val app: Application = application().build()
    val eori = "GB000000000001"
    val notificationMessageKeys: Seq[String] = Seq("key1", "key2")

    implicit val appConfig: AppConfig = app.injector.instanceOf[config.AppConfig]
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val view: Document = Jsoup.parse(
      app.injector.instanceOf[account_not_available].apply(eori, notificationMessageKeys).body
    )
  }
}

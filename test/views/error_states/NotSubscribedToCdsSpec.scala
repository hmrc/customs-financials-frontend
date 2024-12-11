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
import views.html.error_states.not_subscribed_to_cds
import utils.MustMatchers

class NotSubscribedToCdsSpec extends SpecBase with MustMatchers {

  "Not Subscribed to CDS view" should {
    "render correctly" in new Setup {
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.title")) mustBe true
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.heading")) mustBe true
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.already-subscribed-to-cds")) mustBe true
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.already-subscribed-to-cds-guidance-text")) mustBe true
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.subscribe-cds.title")) mustBe true
      htmlText.contains(msg("cf.not-subscribed-to-cds.detail.subscribe-cds.link")) mustBe true
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val appConfig: AppConfig                         = app.injector.instanceOf[config.AppConfig]
    implicit val msg: Messages                                = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val view: Document = Jsoup.parse(
      app.injector.instanceOf[not_subscribed_to_cds].apply().body
    )
    val htmlText       = view.text()
  }
}

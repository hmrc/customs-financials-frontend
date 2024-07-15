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

package views.account_cards

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.account_cards.import_vat_card
import org.scalatest.matchers.must.{Matchers => MustMatchers}

class ImportVatCardSpec extends SpecBase with MustMatchers {

  "view" should {
    "display correct text and guidance" in new Setup {
      viewDoc.text().contains(msgs("cf.import-vat.view-certificates.description")) mustBe true

      val linkElement: Elements = viewDoc.getElementsByClass("vat-link")
      linkElement.get(0).text() mustBe msgs("cf.customs-financials-home.import-vat.title.link")
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val viewDoc: Document = Jsoup.parse(app.injector.instanceOf[import_vat_card].apply().body)
  }

}

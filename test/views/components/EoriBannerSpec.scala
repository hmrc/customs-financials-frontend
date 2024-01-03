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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.eori_banner

class EoriBannerSpec extends SpecBase {

  "view" should {
    "display correct contents" when {
      "eori, companyName and xiEori have values" in new Setup {
        val view: Document = viewDoc(Some(eori), Some(companyName), xiEori)

        val paragraphText: String = view.getElementById("eori-company").text()

        paragraphText.contains(s"${msgs("cf.header.companyName", companyName)} $eori") mustBe true
        paragraphText.contains(xiEori) mustBe true
      }

      "eori and companyName have None but xiEori is empty" in new Setup {
        val view: Document = viewDoc(xiEori = emptyString)

        view.getElementsByClass("govuk-phase-banner").text() mustBe empty
      }

      "eori has some value but companyName and xiEori have no value" in new Setup {
        val view: Document = viewDoc(eori = Some(eori), xiEori = emptyString)

        view.getElementById("eori").text() mustBe s"${msgs("cf.header.eori")} $eori"
      }

      "eori and companyName have some value but xiEori is empty" in new Setup {
        val view: Document = viewDoc(eori = Some(eori), companyName = Some(companyName), xiEori = emptyString)

        val paragraphText: String = view.getElementById("eori-company").text()

        paragraphText.contains(s"${msgs("cf.header.companyName", companyName)} $eori") mustBe true
        paragraphText.contains(xiEori) mustBe false
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)

    val eori = "test_eori"
    val companyName = "test_company"
    val xiEori = "XI12345678"

    def viewDoc(eori: Option[String] = None,
                companyName: Option[String] = None,
                xiEori: String): Document = Jsoup.parse(app.injector.instanceOf[eori_banner].apply(
      eori,
      companyName,
      xiEori
    ).body)
  }
}

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
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.authorised_account_search_company_name

class AuthorisedAccountSearchCompanyNameSpec extends SpecBase {
  "AuthorisedAccountSearchCompanyName view" should {
    "load correctly and display correct guidance" in new Setup {

      val view: Document = Jsoup.parse(app.injector.instanceOf[authorised_account_search_company_name].apply(
        Option("TestCompnay"),
        Option("GBN45365789211"),
        displayLink = true).body)

      view.getElementsByTag("p").html() mustBe
        messages(app)("cf.search.authorities.result.inset2")
    }

    "display correct guidance when company name is unavailable and displayLink is false" in new Setup {

      val view: Document = Jsoup.parse(app.injector.instanceOf[authorised_account_search_company_name].apply(
        None,
        Option("GBN45365789211"),
        displayLink = false).body)

      view.getElementsByTag("p").html() mustBe
        messages(app)("cf.search.authorities.result.inset1")
    }

    "display only GB EORI when only GB EORI is present" in new Setup {
      val view: Document = Jsoup.parse(app.injector.instanceOf[authorised_account_search_company_name].apply(
        Option("TestCompnay"),
        Option("GBN45365789211"),
        displayLink = true).body)

      view.getElementsByTag("p").html() mustBe
        messages(app)("cf.search.authorities.result.inset2")

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.eori.number")

      elements.get(1).getElementsByTag("dd").html() mustBe "GBN45365789211"
    }

    "display only XI EORI when only XI EORI is present" in new Setup {
      val view: Document = Jsoup.parse(app.injector.instanceOf[authorised_account_search_company_name].apply(
        Option("TestCompnay"),
        None,
        displayLink = true,
        Option("XI45365789211")).body)

      view.getElementsByTag("p").html() mustBe
        messages(app)("cf.search.authorities.result.inset2")

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.xiEori.number")

      elements.get(1).getElementsByTag("dd").html() mustBe "XI45365789211"
    }

    "display both GB and XI EORI when both are present" in new Setup {
      val view: Document = Jsoup.parse(app.injector.instanceOf[authorised_account_search_company_name].apply(
        Option("TestCompnay"),
        Option("GBN45365789211"),
        displayLink = true,
        Option("XI45365789211")).body)

      view.getElementsByTag("p").html() mustBe
        messages(app)("cf.search.authorities.result.inset2")

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.eori.number")
      elements.get(1).getElementsByTag("dd").html() mustBe "GBN45365789211"

      elements.get(2).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.xiEori.number")
      elements.get(2).getElementsByTag("dd").html() mustBe "XI45365789211"
    }
  }

  trait Setup {
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
  }
}

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

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.authorised_account_search_panel
import utils.MustMatchers

class AuthorisedAccountSearchPanelSpec extends SpecBase with MustMatchers {

  "AuthorisedAccountSearchPanel view" should {
    "load correctly with correct guidance when using duty deferement" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "DutyDeferment",
            accountNumber,
            Option("16000"),
            Option("17000")
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.DutyDeferment", emptyString).trim

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber
    }

    "load correctly with correct guidance with using CDSCash" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "CDSCash",
            accountNumber,
            Option("16000"),
            Option("17000")
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.CDSCash")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber
    }

    "display accountBalance and related elements when it is present" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "CDSCash",
            accountNumber,
            Option("16000"),
            Option("17000")
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.CDSCash")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber

      elements.get(2).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.balance")
      elements.get(2).getElementsByTag("dd").html() mustBe "£16000"
    }

    "not display accountBalance and related elements when it is not present" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "CDSCash",
            accountNumber,
            None,
            None
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.CDSCash")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber

      intercept[RuntimeException] {
        elements.get(2).getElementsByTag("dt").html() mustBe
          messages(app)("cf.search.authorities.result.account.balance")
        elements.get(2).getElementsByTag("dd").html() mustBe "£16000"
      }
    }

    "display guaranteeBalanceAvailable and related elements when it is present" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "CDSCash",
            accountNumber,
            Option("16000"),
            Option("17000")
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.CDSCash")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber

      elements.get(3).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.guarantee.balance.available")
      elements.get(3).getElementsByTag("dd").html() mustBe "£17000"
    }

    "not display guaranteeBalanceAvailable and related elements when it is not present" in new Setup {
      val view: Document = Jsoup.parse(
        app.injector
          .instanceOf[authorised_account_search_panel]
          .apply(
            "CDSCash",
            accountNumber,
            Option("16000"),
            None
          )
          .body
      )

      val elements: Elements = view.getElementsByClass("govuk-summary-list__row")

      elements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.type")
      elements.get(0).getElementsByTag("dd").html() mustBe
        messages(app)("cf.search.authorities.accountType.CDSCash")

      elements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.account.number")
      elements.get(1).getElementsByTag("dd").html() mustBe accountNumber

      intercept[RuntimeException] {
        elements.get(3).getElementsByTag("dt").html() mustBe
          messages(app)("cf.search.authorities.result.guarantee.balance.available")
        elements.get(3).getElementsByTag("dd").html() mustBe "£17000"
      }
    }
  }

  trait Setup {
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    val accountNumber = "1234567"
  }
}

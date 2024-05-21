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

package views.authorised_to_view

import config.AppConfig
import domain.{Account, AuthorisedBalances, AuthorisedCashAccount, AuthorisedDutyDefermentAccount, AuthorisedGeneralGuaranteeAccount, SearchedAuthorities}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import utils.SpecBase
import views.html.authorised_to_view.authorised_to_view_search_result

class AuthorisedToViewSearchResultSpec extends SpecBase {

  "AuthorisedToViewSearchResult view" should {
    "display the correct title, header and company name" in new SetUp {

      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search_result].apply(
          "1100001",
          Option("GBN45365789211"),
          SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)),
          Option("TestCompany"),
          displayLink = true).body)

      running(app) {
        view.title() mustBe
          s"${
            messages(app)(
              "cf.search.authorities.result.title", "1100001")
          } - ${messages(app)("service.name")} - GOV.UK"

        view.getElementsByTag("h1").html() mustBe
          (messages(app)("cf.search.authorities.result.title", "1100001"))

        val summaryRowElements: Elements = view.getElementsByClass("govuk-summary-list__row")

        summaryRowElements.get(1).getElementsByTag("dt").html() mustBe
          messages(app)("cf.search.authorities.result.company.name")
        summaryRowElements.get(1).getElementsByTag("dd").html() mustBe "TestCompany"
      }
    }

    "display both GB and XI EORI labels if authorities are returned for both" in new SetUp {

      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search_result].apply(
          "1100001",
          Option("GBN45365789211"),
          SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)),
          Option("TestCompany"),
          displayLink = true,
          Option("XI45365789211")).body)

      val summaryRowElements: Elements = view.getElementsByClass("govuk-summary-list__row")

      summaryRowElements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.eori.number")
      summaryRowElements.get(0).getElementsByTag("dd").html() mustBe "GBN45365789211"

      summaryRowElements.get(1).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.xiEori.number")
      summaryRowElements.get(1).getElementsByTag("dd").html() mustBe "XI45365789211"
    }

    "display only GB EORI label if authorities are available only for GB EORI" in new SetUp {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search_result].apply(
          "1100001",
          Option("GBN45365789211"),
          SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)),
          Option("TestCompany"),
          displayLink = true).body)

      val summaryRowElements: Elements = view.getElementsByClass("govuk-summary-list__row")

      summaryRowElements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.eori.number")
      summaryRowElements.get(0).getElementsByTag("dd").html() mustBe "GBN45365789211"
    }

    "display only XI EORI label if authorities are available for XI EORI" in new SetUp {
      val view: Document = Jsoup.parse(
        app.injector.instanceOf[authorised_to_view_search_result].apply(
          "1100001",
          None,
          SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)),
          Option("TestCompany"),
          displayLink = true,
          Option("XI45365789211")).body)

      val summaryRowElements: Elements = view.getElementsByClass("govuk-summary-list__row")

      summaryRowElements.get(0).getElementsByTag("dt").html() mustBe
        messages(app)("cf.search.authorities.result.xiEori.number")
      summaryRowElements.get(0).getElementsByTag("dd").html() mustBe "XI45365789211"
    }
  }

  trait SetUp {
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
      AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

    val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
      AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"),
        Some(AuthorisedBalances("100.0", "200.0")))

    val cashAccount: AuthorisedCashAccount =
      AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
  }
}

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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.html.account_cards.duty_deferment_balance_details
import org.scalatest.matchers.must.{Matchers => MustMatchers}

class DutyDefermentBalanceDetailsSpec extends SpecBase with MustMatchers {

  "view" should {
    "display correct contents" when {
      "accountLimit, guaranteeLimit, guaranteeLimitRemaining have value and viewBalances is true" in new Setup {
        val view: Document = viewDoc(
          accountNumber,
          Some(accountLimit),
          Some(guaranteeLimit),
          Some(guaranteeLimitRemaining),
          viewBalances = true)

        val accountLimitElement: String = view.getElementById(s"account-limit-$accountNumber").text()
        val accountLimitHintElement: String = view.getElementsByClass("govuk-hint").text()

        accountLimitElement.contains(
          msgs("cf.duty-deferment.account.card.account.limit", accountLimit)) mustBe true

        accountLimitHintElement.contains(msgs("cf.duty-deferment.account.card.account.info-text")) mustBe true

        val guaranteeLimitElement: String = view.getElementById(s"guarantee-limit-$accountNumber").text()

        guaranteeLimitElement.contains(
          msgs("cf.duty-deferment.account.card.guarantee.limit", guaranteeLimit)) mustBe true

        val guaranteeLimitRemainingElement: String =
          view.getElementById(s"guarantee-limit-remaining-$accountNumber").text()

        guaranteeLimitRemainingElement.contains(
          msgs(
            "cf.duty-deferment.account.card.guarantee.limit.remaining", guaranteeLimitRemaining)) mustBe true
      }

      "accountLimit, guaranteeLimit, guaranteeLimitRemaining have value and viewBalances is false" in new Setup {
        val view: Document = viewDoc(
          accountNumber,
          Some(accountLimit),
          Some(guaranteeLimit),
          Some(guaranteeLimitRemaining))

        val accountLimitElement: String = view.getElementById(s"account-limit-$accountNumber").text()
        val accountLimitHintElement: String = view.getElementsByClass("govuk-hint").text()

        accountLimitElement.contains(
          msgs("cf.duty-deferment.account.card.account.limit", accountLimit)) mustBe true

        accountLimitHintElement.contains(msgs("cf.duty-deferment.account.card.account.info-text")) mustBe true

        val guaranteeLimitElement: String = view.getElementById(s"guarantee-limit-$accountNumber").text()

        guaranteeLimitElement.contains(
          msgs("cf.duty-deferment.account.card.guarantee.limit", guaranteeLimit)) mustBe true
      }

      "only accountLimit has value and viewBalances is false" in new Setup {
        val view: Document = viewDoc(accountNumber, Some(accountLimit))

        val accountLimitElement: String = view.getElementById(s"account-limit-$accountNumber").text()
        val accountLimitHintElement: String = view.getElementsByClass("govuk-hint").text()

        accountLimitElement.contains(msgs("cf.duty-deferment.account.card.account.limit", accountLimit)) mustBe true

        accountLimitHintElement.contains(msgs("cf.duty-deferment.account.card.account.info-text")) mustBe true
      }

      "only guaranteeLimit has value and viewBalances is false" in new Setup {
        val view: Document = viewDoc(accountNumber = accountNumber, guaranteeLimit = Some(guaranteeLimit))

        val guaranteeLimitElement: String = view.getElementById(s"guarantee-limit-$accountNumber").text()

        guaranteeLimitElement.contains(
          msgs("cf.duty-deferment.account.card.guarantee.limit", guaranteeLimit)) mustBe true
      }

      "only guaranteeLimitRemaining has value and viewBalances is true" in new Setup {
        val view: Document = viewDoc(
          accountNumber = accountNumber,
          guaranteeLimitRemaining = Some(guaranteeLimitRemaining),
          viewBalances = true)

        val guaranteeLimitRemainingElement: String =
          view.getElementById(s"guarantee-limit-remaining-$accountNumber").text()

        guaranteeLimitRemainingElement.contains(
          msgs(
            "cf.duty-deferment.account.card.guarantee.limit.remaining", guaranteeLimitRemaining)) mustBe true
      }

      "accountLimit, guaranteeLimit, guaranteeLimitRemaining are None and viewBalances is false" in new Setup {
        val view: Document = viewDoc(accountNumber = accountNumber)

        view.text().contains(msgs("cf.duty-deferment.account.card.account.limit", accountLimit)) mustBe false

        view.text().contains(msgs("cf.duty-deferment.account.card.guarantee.limit", guaranteeLimit)) mustBe false

        view.text().contains(msgs(
          "cf.duty-deferment.account.card.guarantee.limit.remaining", guaranteeLimitRemaining)) mustBe false
      }

      "accountLimit, guaranteeLimit, guaranteeLimitRemaining are None and viewBalances is true" in new Setup {
        val view: Document = viewDoc(accountNumber = accountNumber, viewBalances = true)

        view.text().contains(msgs("cf.duty-deferment.account.card.account.limit", accountLimit)) mustBe false

        view.text().contains(msgs("cf.duty-deferment.account.card.guarantee.limit", guaranteeLimit)) mustBe false

        view.text().contains(msgs(
          "cf.duty-deferment.account.card.guarantee.limit.remaining", guaranteeLimitRemaining)) mustBe false
      }
    }
  }

  trait Setup {
    val app: Application = application().build()
    implicit val msgs: Messages = messages(app)

    val accountNumber: String = "12345678"
    val accountLimit: String = "5400"
    val guaranteeLimit: String = "460"
    val guaranteeLimitRemaining: String = "500"
    val viewBalances: Boolean = false

    def viewDoc(accountNumber: String,
                accountLimit: Option[String] = None,
                guaranteeLimit: Option[String] = None,
                guaranteeLimitRemaining: Option[String] = None,
                viewBalances: Boolean = false): Document =
      Jsoup.parse(app.injector.instanceOf[duty_deferment_balance_details].apply(
        accountNumber,
        accountLimit,
        guaranteeLimit,
        guaranteeLimitRemaining,
        viewBalances
      ).body)
  }
}

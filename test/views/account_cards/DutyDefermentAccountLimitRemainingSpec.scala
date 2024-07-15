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

import domain.{AccountStatusClosed, DefermentAccountAvailable, DutyDefermentAccount, DutyDefermentBalance}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import views.helpers.Formatters
import views.html.account_cards.duty_deferment_account_limit_remaining
import utils.MustMatchers

class DutyDefermentAccountLimitRemainingSpec extends SpecBase with MustMatchers {

  "view" should {

    "display correct contents" when {
      "periodAvailableAccountBalance has some value" in new Setup {
        val availableAccountBalanceText: String = viewDoc(
          ddAccount1WithEori,
          periodAvailableBalance = false).getElementsByClass(
          "accountLimitRemaining search-results__item").text()

        availableAccountBalanceText.contains(msgs("cf.account.authorized-to-view.account-limit-remaining"))
        availableAccountBalanceText.contains(Formatters.formatCurrencyAmount(periodAvailableAccountBalance))
      }

      "periodAvailableGuaranteeBalance has some value" in new Setup {
        val availableGuaranteeBalanceText: String = viewDoc(
          ddAccount1WithEori,
          periodAvailableBalance = false).getElementsByClass(
          "guaranteeLimitRemaining search-results__item").text()

        availableGuaranteeBalanceText.contains(msgs("cf.account.authorized-to-view.guarantee-limit-remaining"))
        availableGuaranteeBalanceText.contains(Formatters.formatCurrencyAmount(periodAvailableGuaranteeBalance))
      }

      "periodAvailableBalance is true" in new Setup {
        val view: Document = viewDoc(ddAccount1WithEori, periodAvailableBalance = true)

        val elements: Elements = view.getElementsByClass("limit-remaining")
        elements.size() mustBe 1
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)

    val periodGuaranteeLimit: BigDecimal = BigDecimal(100.0)
    val periodAccountLimit: BigDecimal = BigDecimal(100.0)
    val periodAvailableGuaranteeBalance: BigDecimal = BigDecimal(100.0)
    val periodAvailableAccountBalance: BigDecimal = BigDecimal(100.0)

    val accountNumber = "DAN01234"
    val eori = "test_eori"

    val ddAccount1WithEori: DutyDefermentAccount =
      DutyDefermentAccount(
        accountNumber,
        eori,
        isNiAccount = false,
        AccountStatusClosed, DefermentAccountAvailable,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false)

    def viewDoc(account: DutyDefermentAccount, periodAvailableBalance: Boolean): Document =
      Jsoup.parse(app.injector.instanceOf[duty_deferment_account_limit_remaining].apply(
        account,
        periodAvailableBalance
      ).body)
  }
}

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
import domain.{
  AccountLink, AccountStatusClosed, AccountStatusOpen, AccountStatusSuspended, CDSAccounts,
  DefermentAccountAvailable, DirectDebitMandateCancelled, DutyDefermentAccount, DutyDefermentBalance
}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import viewmodels.{DutyDefermentAccountsViewModel, FinancialsHomeModel}
import views.helpers.Formatters
import views.html.account_cards.duty_deferment_account_cards

import java.time.LocalDateTime

class DutyDefermentAccountCardsSpec extends SpecBase {

  "view" should {

    "display correct contents" when {
      "model has dutyDefermentAccounts and has only 1 DutyDefermentAccount for the eori" in new Setup {
        val model: FinancialsHomeModel = FinancialsHomeModel(eori1, None, accounts, Nil, accountLinks)

        viewDoc(model).getElementsByTag("h2").text() mustBe
          msgs("cf.customs-financials-home.duty-deferment.title")

        viewDoc(model).getElementById("duty-deferment-balances-warning").text() mustBe
          msgs("cf.duty-deferment.outOfDateBalance.chiefText")

        viewDoc(model).getElementsByTag("h3").text() mustBe
          s"${msgs("cf.account")} $dan1 ${msgs("cf.account.status.aria.AccountStatusClosed")}"

        viewDoc(model).getElementsByClass("card-header").text() mustBe
          s"${msgs("cf.account")} $dan1 ${msgs("cf.account.status.aria.AccountStatusClosed")} ${
            msgs("cf.account.status.AccountStatusClosed")
          }"

        val ddBalanceElem: Element = viewDoc(model).getElementById(s"duty-deferment-balance-$dan1")

        ddBalanceElem.text().contains(Formatters.formatCurrencyAmount(periodAccountLimit))
        ddBalanceElem.text().contains(msgs("cf.available"))

        val statementElement: Elements = viewDoc(model).getElementsByClass("card-footer__links")
        statementElement.text().contains(msgs("cf.accounts.viewStatements"))
        statementElement.text().contains(msgs("cf.accounts.label.dan", dan1))

      }

      "model has dutyDefermentAccounts and has more than one DutyDefermentAccount for eori" in new Setup {
        val model: FinancialsHomeModel = FinancialsHomeModel(eori1, None, accountsWithEori1, Nil, accountLinks)

        viewDoc(model).getElementsByTag("h2").text() mustBe
          msgs("cf.customs-financials-home.duty-deferment.title2")

        viewDoc(model).getElementById("duty-deferment-balances-warning").text() mustBe
          msgs("cf.duty-deferment.outOfDateBalance.chiefText")
      }

      "model has one DutyDefermentAccount for the eori, with AccountStatusSuspended and NI account" in new Setup {
        val model: FinancialsHomeModel =
          FinancialsHomeModel(eori1, None, accountsWithEori1WithNiAccount, Nil, accountLinksWithNiAccount)

        viewDoc(model).getElementsByTag("h2").text() mustBe
          msgs("cf.customs-financials-home.duty-deferment.title2")

        viewDoc(model).getElementById("duty-deferment-balances-warning").text() mustBe
          msgs("cf.duty-deferment.outOfDateBalance.chiefText")

        viewDoc(model).getElementsByTag("h3").text().contains(
          s"${msgs("cf.NiAccount")} $dan1 ${msgs("cf.account.status.aria.AccountStatusSuspended")}")

        val ddDirectDebitElementHtml: String = viewDoc(model).html()

        ddDirectDebitElementHtml.contains(msgs("cf.duty-deferment.warning"))
        ddDirectDebitElementHtml.contains(msgs("cf.duty-deferment.dd.info"))
        ddDirectDebitElementHtml.contains(msgs("cf.duty-deferment.dd.setup.text1"))
        ddDirectDebitElementHtml.contains(msgs("cf.duty-deferment.dd.setup.new.account"))
        ddDirectDebitElementHtml.contains(msgs("cf.duty-deferment.dd.setup.text2"))
      }
    }
  }

  trait Setup {
    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val eori1 = "test_eori_1"
    val eori2 = "test_eori_2"
    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    val periodGuaranteeLimit: BigDecimal = BigDecimal(100.0)
    val periodAccountLimit: BigDecimal = BigDecimal(100.0)
    val periodAvailableGuaranteeBalance: BigDecimal = BigDecimal(100.0)
    val periodAvailableAccountBalance: BigDecimal = BigDecimal(100.0)

    val ddAccount1WithEori1: DutyDefermentAccount =
      DutyDefermentAccount(dan1, eori1, isNiAccount = false, AccountStatusClosed, DefermentAccountAvailable,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false)

    val ddAccount1WithEori1WithNIAccountAndDirectDebitCancelled: DutyDefermentAccount =
      DutyDefermentAccount(dan1, eori1, isNiAccount = true, AccountStatusSuspended, DirectDebitMandateCancelled,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false)

    val ddAccount1WithEori2: DutyDefermentAccount =
      DutyDefermentAccount(dan2, eori2, isNiAccount = false, AccountStatusClosed, DefermentAccountAvailable,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false)

    val accounts: Seq[CDSAccounts] = Seq(CDSAccounts(eori1, None, Seq(ddAccount1WithEori1, ddAccount1WithEori2)))
    val accountsWithEori1: Seq[CDSAccounts] =
      Seq(CDSAccounts(eori1, None, Seq(ddAccount1WithEori1, ddAccount1WithEori1)))

    val accountsWithEori1WithNiAccount: Seq[CDSAccounts] =
      Seq(
        CDSAccounts(
          eori1,
          None,
          Seq(
            ddAccount1WithEori1WithNIAccountAndDirectDebitCancelled,
            ddAccount1WithEori1WithNIAccountAndDirectDebitCancelled)
        )
      )

    val accountLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = "sessionId",
        eori1,
        isNiAccount = false,
        accountNumber = dan1,
        linkId = "linkId",
        accountStatus = AccountStatusOpen,
        accountStatusId = Option(DefermentAccountAvailable),
        lastUpdated = LocalDateTime.now())
    )

    val accountLinksWithNiAccount: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = "sessionId",
        eori1,
        isNiAccount = true,
        accountNumber = dan1,
        linkId = "linkId",
        accountStatus = AccountStatusSuspended,
        accountStatusId = Option(DefermentAccountAvailable),
        lastUpdated = LocalDateTime.now())
    )

    def viewDoc(model: FinancialsHomeModel): Document =
      Jsoup.parse(app.injector.instanceOf[duty_deferment_account_cards].apply(
        DutyDefermentAccountsViewModel(model)).body)
  }
}

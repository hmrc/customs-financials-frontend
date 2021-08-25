/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.views.components

import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.{AccountCancelled, AccountLink, AccountStatusClosed, AccountStatusOpen, AccountStatusPending, AccountStatusSuspended, CDSAccounts, ChangeOfLegalEntity, DebitRejectedAccountClosedOrTransferred, DebitRejectedReferToDrawer, DefermentAccountAvailable, DirectDebitMandateCancelled, DutyDefermentAccount, DutyDefermentBalance, GuaranteeCancelledGuarantorsRequest, GuaranteeCancelledTradersRequest, GuaranteeExceeded, ReturnedMailOther}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.viewmodels.FinancialsHomeModel
import uk.gov.hmrc.customs.financials.views.html.components.duty_deferment_account_card

class DutyDefermentAccountCardSpec extends SpecBase {

  "Duty Deferment Account Card" should {

    "be marked with class 'duty-deferment-account'" in new Setup {
      running(app) {
        content().getElementsByClass("duty-deferment-account").isEmpty mustBe false
      }
    }

    "be marked with class 'card-header'" in new Setup {
      running(app) {
        content().getElementsByTag("header").hasClass("card-header") mustBe true
      }
    }

    "include available account balance" in new Setup {
      running(app) {
        content().getElementsByTag("p").hasClass("account-balance-status-open") mustBe true
        content().getElementsByTag("p").first().text mustBe "£99.01 available"
      }
    }

    "include account limit remaining" in new Setup {
      running(app) {
        content().getElementById("account-limit-123456").text mustBe "Your Account limit: £499"
      }
    }

    "include a link to the duty deferment account page" in new Setup {
      running(app) {
        val expectedUrl = "/customs/payment-records/duty-deferment/0123456789"
        content().containsLink(expectedUrl) mustBe true
      }
    }

    "include a link to the duty deferment payment details page" in new Setup {
      running(app) {
        val expectedUrl = "https://www.gov.uk/guidance/top-up-your-duty-deferment-account-using-the-customs-declaration-service"
        content().containsLink(expectedUrl) mustBe true
      }
    }

    "include a link to the duty deferment contact details page when status is open" in new Setup {
      running(app) {
        val expectedUrl = contactDetailsLink1
        content().containsLink(expectedUrl) mustBe true
      }
    }

    "include a link to the duty deferment contact details page when status is suspended" in new Setup {
      running(app) {
        val expectedUrl = contactDetailsLink1
        content(dutyDefermentAccountSuspended).containsLink(expectedUrl) mustBe true
      }
    }

    "not include a link to the duty deferment contact details page when status is closed" in new Setup {
      running(app) {
        val expectedUrl = contactDetailsLink1
        content(dutyDefermentAccountClosed).containsLinkWithText(expectedUrl, "Account details") mustBe false
      }
    }

    "not include a link to the duty deferment contact details page when status is pending" in new Setup {
      running(app) {
        val expectedUrl = contactDetailsLink1
        content(dutyDefermentAccountPending).containsLinkWithText(expectedUrl, "Account details") mustBe false
      }
    }

    "not include account limit remaining" in new Setup {
      running(app) {
        content(dutyDefermentAccountWithoutBalances).getElementsByClass("account-limit-remaining").size() mustBe 0
      }
    }

    "not include guarantee limit remaining" in new Setup {
      running(app) {
        content(dutyDefermentAccountWithoutBalances).getElementsByClass("account-limit-remaining").size() mustBe 0
      }
    }

    "render progress bar component" in new Setup {
      running(app) {
        content().getElementsByClass("progress-bar").isEmpty mustBe false
      }
    }

    "include the hidden text for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance).getElementsByTag("span")
          .hasClass("govuk-visually-hidden") mustBe true
      }
    }

    "include the aria-hidden attribute for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance).getElementsByTag("span")
          .hasClass("custom-card__balance-amount--negative") mustBe true
      }
    }

    "include the color class for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance).getElementsByTag("span")
          .hasClass("custom-card__balance-amount--negative") mustBe true
      }
    }

    "include account limit remaining with negative account balance added" in new Setup {
      running(app) {
        content(accountWithNegativeBalance).getElementById("account-limit-123456").text mustBe "Your Account limit: £500"
      }
    }

    "not render progress bar component for negative account balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance).getElementsByClass("progress-bar").isEmpty mustBe true
      }
    }
  }

  "Duty Deferment Account Card" when {
    "account is open" should {
      "render progress bar component" in new Setup {
        running(app) {
          content(dutyDefermentAccountOpen).getElementsByClass("progress-bar").isEmpty mustBe false
        }
      }

      "not display open account status" in new Setup {
        running(app) {
          content(dutyDefermentAccountOpen).getElementsByClass("duty-deferment-account-status").isEmpty mustBe true
        }
      }
    }

    "account is pending" should {
      "display the pending message when AccountStatus is 'Pending' and AccountStatusID is DefermentAccountAvailable" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).getElementsByTag("h4").hasClass("account-status-pending") mustBe true
        }
      }

      "hidden status-description for screen readers" in new Setup {
        running(app) {
          val status = content(dutyDefermentAccountPending).select("h4.duty-deferment-status").first
          status.getElementsByTag("span").hasClass("govuk-visually-hidden") mustBe true
        }
      }

      "not display limit bar" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }

      "display pending warning if accountStatus is 'Pending'" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).getElementsContainingText("You cannot use this account on CDS yet").isEmpty mustBe false
        }
      }

      "not display pending warning if accountStatus is not 'Pending'" in new Setup {
        running(app) {
          content(dutyDefermentAccountOpen).getElementsContainingText("You cannot use this account on CDS yet").isEmpty mustBe true
        }
      }

      "not display set up direct debit if accountStatus is 'Pending'" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display set up direct debit if accountStatus is not 'Pending' and accountStatusId is not 'DirectDebitMandateCancelled'" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "display set up direct debit if accountStatus is not 'Pending' and accountStatusId is 'DirectDebitMandateCancelled'" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID4).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe true
        }
      }

      "display will be available if account balance is not 0" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending).getElementsByTag("p").hasClass("account-balance-status-pending") mustBe true
          content(dutyDefermentAccountPending).getElementsByTag("p").first().text mustBe "£99.01 will be available"
        }
      }

      "not display will be available if account balance is 0" in new Setup {
        running(app) {
          content(dutyDefermentAccountPendingZeroBalance).getElementsByTag("p").hasClass("account-balance-status-pending") mustBe true
          content(dutyDefermentAccountPendingZeroBalance).getElementsByTag("p").first().text mustBe "£0"
        }
      }

    }

    "account is suspended" should {
      "display account status has  been suspended" in new Setup {
        running(app) {
          content(dutyDefermentAccountSuspended).getElementsByTag("h4").hasClass("account-status-suspended") mustBe true
        }
      }

      "hidden status-description for screen readers" in new Setup {
        running(app) {
          val status = content(dutyDefermentAccountSuspended).select("h4.duty-deferment-status").first
          status.getElementsByTag("span").hasClass("govuk-visually-hidden") mustBe true
        }
      }

      "not display limit bar" in new Setup {
        running(app) {

          content(dutyDefermentAccountSuspended).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }

      "display direct debit setup link if accountStatusId is DirectDebitMandateCancelled" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID4).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe true
        }
      }

      "not display direct debit setup link if accountStatusId is DefermentAccountAvailable" in new Setup {
        running(app) {
          content().containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is ChangeOfLegalEntity" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID1).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is GuaranteeCancelledGuarantorsRequest" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID2).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is GuaranteeCancelledTradersRequest" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID3).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is DebitRejectedAccountClosedOrTransferred" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID5).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is DebitRejectedReferToDrawer" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID6).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is ReturnedMailOther" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID7).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is GuaranteeExceeded" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID8).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is AccountCancelled" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID9).containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
    }

    "account is closed" should {

      "display account status has  been closed" in new Setup {
        running(app) {

          content(dutyDefermentAccountClosed).getElementsByTag("h4").hasClass("account-status-closed") mustBe true
        }

      }

      "hidden status-description for screen readers" in new Setup {
        running(app) {

          val status = content(dutyDefermentAccountClosed).select("h4.duty-deferment-status").first
          status.getElementsByTag("span").hasClass("govuk-visually-hidden") mustBe true
        }
      }

      "not display limit bar" in new Setup {
        running(app) {
          content(dutyDefermentAccountClosed).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }


      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountClosed).containsLinkWithText(ddSetupLink, "set up a new Direct Debit.") mustBe false
        }
      }
    }

    "DutyDefermentAccount account is suspended and isleOfMan flag set true" should {
      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountSuspendedIsleOfMan).containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
        }
      }
    }

    "DutyDefermentAccount account is open and isleOfMan flag set true" should {
      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusOpenIsleOfMan).containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
        }
      }

      "DutyDefermentAccount account is closed and isleOfMan flag set true" should {
        "not include a a link to the duty deferment direct debit setup details" in new Setup {
          running(app) {
            content(dutyDefermentAccountStatusClosedIsleOfMan).containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
          }
        }
      }
    }
  }

  trait Setup extends I18nSupport {
    val eori = "owner"
    val dan = "123456"
    val topUpLink1 = "/topup-link/0123456789"
    val topUpLink2 = "/topup-link/1111111111"
    val ddSetupLink = "/customs/payment-records/duty-deferment/direct-debit-setup/0123456789"
    val contactDetailsLink1 = "/customs/payment-records/duty-deferment/contact-details/0123456789"
    val contactDetailsLink2 = "/contact-details-link/1111111111"
    val otherEori = "other"
//    val accountLinks = Map[(String, String), String]((eori, dan) -> link)
    val dutyDefermentAccount = DutyDefermentAccount(dan, eori, AccountStatusOpen, DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(999)), Some(BigDecimal(499)), Some(BigDecimal(299)), Some(BigDecimal(99.01))), viewBalanceIsGranted = true, isIsleOfMan = false) // scalastyle:ignore magic.number

    val accountWithNegativeBalance = DutyDefermentAccount(dan, eori, AccountStatusOpen, DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(999)), Some(BigDecimal(500)), Some(BigDecimal(299)), Some(BigDecimal(-100))), viewBalanceIsGranted = true, isIsleOfMan = false) // scalastyle:ignore magic.number

    val dutyDefermentAccountWithoutBalances = DutyDefermentAccount(dan, eori, AccountStatusOpen, DefermentAccountAvailable,
      DutyDefermentBalance(None, None, None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dutyDefermentAccountOpen = dutyDefermentAccount.copy(status = AccountStatusOpen)
    val dutyDefermentAccountSuspended = dutyDefermentAccount.copy(status = AccountStatusSuspended)
    val dutyDefermentAccountClosed = dutyDefermentAccount.copy(status = AccountStatusClosed)
    val dutyDefermentAccountPending = dutyDefermentAccount.copy(status = AccountStatusPending)
    val dutyDefermentAccountSuspendedIsleOfMan = dutyDefermentAccount.copy(status = AccountStatusSuspended, isIsleOfMan = true)
    val dutyDefermentAccountStatusOpenIsleOfMan = dutyDefermentAccount.copy(status = AccountStatusOpen, isIsleOfMan = true)
    val dutyDefermentAccountStatusClosedIsleOfMan = dutyDefermentAccount.copy(status = AccountStatusClosed, isIsleOfMan = true)

    val dutyDefermentAccountStatusID1 = dutyDefermentAccount.copy(statusId = ChangeOfLegalEntity)
    val dutyDefermentAccountStatusID2 = dutyDefermentAccount.copy(statusId = GuaranteeCancelledGuarantorsRequest)
    val dutyDefermentAccountStatusID3 = dutyDefermentAccount.copy(statusId = GuaranteeCancelledTradersRequest)
    val dutyDefermentAccountStatusID4 = dutyDefermentAccount.copy(statusId = DirectDebitMandateCancelled)
    val dutyDefermentAccountStatusID5 = dutyDefermentAccount.copy(statusId = DebitRejectedAccountClosedOrTransferred)
    val dutyDefermentAccountStatusID6 = dutyDefermentAccount.copy(statusId = DebitRejectedReferToDrawer)
    val dutyDefermentAccountStatusID7 = dutyDefermentAccount.copy(statusId = ReturnedMailOther)
    val dutyDefermentAccountStatusID8 = dutyDefermentAccount.copy(statusId = GuaranteeExceeded)
    val dutyDefermentAccountStatusID9 = dutyDefermentAccount.copy(statusId = AccountCancelled)

    val dutyDefermentAccountPendingZeroBalance = dutyDefermentAccountPending.copy(balances = DutyDefermentBalance(Some(BigDecimal(999)), Some(BigDecimal(499)), Some(BigDecimal(299)), Some(BigDecimal(00.00))))

    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]


    val accounts: Seq[CDSAccounts] = Seq(
      CDSAccounts(eori, Seq(dutyDefermentAccount, dutyDefermentAccountWithoutBalances))
    )
    val accountLinks = Seq(
      AccountLink(sessionId = "sessionId", eori, accountNumber = dan, linkId = "0123456789", accountStatus = AccountStatusOpen, accountStatusId = Option(DefermentAccountAvailable), lastUpdated = DateTime.now)
    )

    val model = FinancialsHomeModel(eori,accounts = accounts, accountLinks = accountLinks, notificationMessageKeys = Seq.empty)

    def content(dutyDefermentAccount: DutyDefermentAccount = dutyDefermentAccount) = Jsoup.parse(app.injector.instanceOf[duty_deferment_account_card]
      .apply(dutyDefermentAccount, model).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }

}

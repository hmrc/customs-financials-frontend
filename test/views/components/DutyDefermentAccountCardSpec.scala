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
import domain.{
  AccountCancelled, AccountLink, AccountStatusClosed, AccountStatusOpen, AccountStatusPending, AccountStatusSuspended,
  CDSAccounts, ChangeOfLegalEntity, DebitRejectedAccountClosedOrTransferred, DebitRejectedReferToDrawer,
  DefermentAccountAvailable, DirectDebitMandateCancelled, DutyDefermentAccount, DutyDefermentBalance,
  GuaranteeCancelledGuarantorsRequest, GuaranteeCancelledTradersRequest, GuaranteeExceeded, ReturnedMailOther
}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase
import utils.TestData.{BALANCE_299, BALANCE_499, BALANCE_500, BALANCE_999, NEGATIVE_BALANCE_100}
import viewmodels.{DutyDefermentAccountsViewModel, FinancialsHomeModel}
import views.html.account_cards.duty_deferment_account_cards
import utils.MustMatchers

import java.time.LocalDateTime

class DutyDefermentAccountCardSpec extends SpecBase with MustMatchers {

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
        content().getElementById("duty-deferment-balance-123456").text mustBe "£99.01 available"
      }
    }

    "include account limit remaining" in new Setup {
      running(app) {
        content().getElementById("account-limit-123456").text mustBe "Your Account limit: £499"
      }
    }

    "include a link to the duty deferment account page" in new Setup {
      running(app) {
        val expectedUrl = "http://localhost:9397/customs/duty-deferment/0123456789/account"
        content().containsLink(expectedUrl) mustBe true
      }
    }

    "include a link to the duty deferment payment details page" in new Setup {
      running(app) {
        val expectedUrl =
          "https://www.gov.uk/guidance/top-up-your-duty-deferment-account-using-the-customs-declaration-service"

        content().containsLink(expectedUrl) mustBe true
      }
    }

    "include a link to the duty deferment contact details page when status is open" in new Setup {
      running(app) {
        val expectedUrl = ddSetupLink
        content().containsLink(expectedUrl) mustBe true
      }
    }

    "not include a link to duty deferment contact details when status is suspended & status id is 4" in new Setup {
      running(app) {
        val expectedUrl = contactDetailsLink1
        content(dutyDefermentAccountSuspendedWithStatusId4).containsLink(expectedUrl) mustBe false
      }
    }

    "include a link to the duty deferment contact details when status is suspended & statusid is not 4" in new Setup {
      running(app) {
        val expectedUrl = ddSetupLink
        content(dutyDefermentAccountSuspendedWithStatusId7).containsLink(expectedUrl) mustBe true
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
        content(dutyDefermentAccountWithoutBalances)
          .getElementsByClass("account-limit-remaining")
          .size() mustBe 0
      }
    }

    "not include guarantee limit remaining" in new Setup {
      running(app) {
        content(dutyDefermentAccountWithoutBalances)
          .getElementsByClass("account-limit-remaining")
          .size() mustBe 0
      }
    }

    "include the hidden text for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance)
          .getElementsByTag("span")
          .hasClass("govuk-visually-hidden") mustBe true
      }
    }

    "include the aria-hidden attribute for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance)
          .getElementsByTag("span")
          .hasClass("custom-card__balance-amount--negative") mustBe true
      }
    }

    "include the color class for negative balance" in new Setup {
      running(app) {
        content(accountWithNegativeBalance)
          .getElementsByTag("span")
          .hasClass("custom-card__balance-amount--negative") mustBe true
      }
    }

    "include account limit remaining with negative account balance added" in new Setup {
      running(app) {
        content(accountWithNegativeBalance)
          .getElementById("account-limit-123456")
          .text mustBe "Your Account limit: £500"
      }
    }
  }

  "Duty Deferment Account Card" when {
    "account is open" should {
      "not display open account status" in new Setup {
        running(app) {
          content(dutyDefermentAccountOpen)
            .getElementsByClass("duty-deferment-account-status")
            .isEmpty mustBe true
        }
      }
    }

    "account is pending" should {
      "hidden status-description for screen readers" in new Setup {
        running(app) {
          val status = content(dutyDefermentAccountPending).select(".duty-deferment-account").first
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
          content(dutyDefermentAccountPending)
            .getElementsContainingText("You cannot use this account on CDS yet")
            .isEmpty mustBe false
        }
      }

      "not display pending warning if accountStatus is not 'Pending'" in new Setup {
        running(app) {
          content(dutyDefermentAccountOpen)
            .getElementsContainingText("You cannot use this account on CDS yet")
            .isEmpty mustBe true
        }
      }

      "not display set up direct debit if accountStatus is 'Pending'" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not show direct debit if accountStatus & accountStatusId are invalid" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "display will be available if account balance is not 0" in new Setup {
        running(app) {
          content(dutyDefermentAccountPending)
            .getElementById("duty-deferment-balance-123456")
            .text mustBe "£99.01 will be available"
        }
      }

      "not display will be available if account balance is 0" in new Setup {
        running(app) {
          content(dutyDefermentAccountPendingZeroBalance)
            .getElementById("duty-deferment-balance-123456")
            .text mustBe "£0"
        }
      }

    }

    "account is suspended" should {
      "not display limit bar" in new Setup {
        running(app) {
          content(dutyDefermentAccountSuspended).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }

      "display direct debit setup link if accountStatusId is DirectDebitMandateCancelled" in new Setup {
        running(app) {
          content(dutyDefermentAccountSuspendedWithStatusId4)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe true
        }
      }

      "not display direct debit setup link if accountStatusId is DefermentAccountAvailable" in new Setup {
        running(app) {
          content().containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is ChangeOfLegalEntity" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID1)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is GuaranteeCancelledGuarantorsRequest" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID2)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }

      "not display direct debit setup link if accountStatusId is GuaranteeCancelledTradersRequest" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID3)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is DebitRejectedAccountClosedOrTransferred" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID5)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is DebitRejectedReferToDrawer" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID6)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is ReturnedMailOther" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID7)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is GuaranteeExceeded" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID8)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
      "not display direct debit setup link if accountStatusId is AccountCancelled" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusID9)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit") mustBe false
        }
      }
    }

    "account is closed" should {
      "not display limit bar" in new Setup {
        running(app) {
          content(dutyDefermentAccountClosed).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }

      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountClosed)
            .containsLinkWithText(ddSetupLink, "set up a new Direct Debit.") mustBe false
        }
      }
    }

    "DutyDefermentAccount account is suspended and isleOfMan flag set true" should {
      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountSuspendedIsleOfMan)
            .containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
        }
      }
    }

    "DutyDefermentAccount account is open and isleOfMan flag set true" should {
      "not include a a link to the duty deferment direct debit setup details" in new Setup {
        running(app) {
          content(dutyDefermentAccountStatusOpenIsleOfMan)
            .containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
        }
      }

      "DutyDefermentAccount account is closed and isleOfMan flag set true" should {
        "not include a a link to the duty deferment direct debit setup details" in new Setup {
          running(app) {
            content(dutyDefermentAccountStatusClosedIsleOfMan)
              .containsLinkWithText(ddSetupLink, "set up a Direct Debit") mustBe false
          }
        }
      }
    }
  }

  trait Setup extends I18nSupport {
    val eori                        = "owner"
    val dan                         = "123456"
    val companyName: Option[String] = Some("Company Name 1")
    val topUpLink1                  = "/topup-link/0123456789"
    val topUpLink2                  = "/topup-link/1111111111"
    val ddSetupLink                 = "http://localhost:9397/customs/duty-deferment/0123456789/direct-debit"
    val contactDetailsLink1         = "http://localhost:9397/customs/duty-deferment/0123456789/contact-details"
    val otherEori                   = "other"

    val dutyDefermentAccount: DutyDefermentAccount = DutyDefermentAccount(
      dan,
      eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_999)),
        Some(BigDecimal(BALANCE_499)),
        Some(BigDecimal(BALANCE_299)),
        Some(BigDecimal(99.01))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val accountWithNegativeBalance: DutyDefermentAccount = DutyDefermentAccount(
      dan,
      eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_999)),
        Some(BigDecimal(BALANCE_500)),
        Some(BigDecimal(BALANCE_299)),
        Some(BigDecimal(NEGATIVE_BALANCE_100))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dutyDefermentAccountWithoutBalances: DutyDefermentAccount =
      DutyDefermentAccount(
        dan,
        eori,
        isNiAccount = false,
        AccountStatusOpen,
        DefermentAccountAvailable,
        DutyDefermentBalance(None, None, None, None),
        viewBalanceIsGranted = true,
        isIsleOfMan = false
      )

    val dutyDefermentAccountOpen: DutyDefermentAccount      = dutyDefermentAccount.copy(status = AccountStatusOpen)
    val dutyDefermentAccountSuspended: DutyDefermentAccount = dutyDefermentAccount.copy(status = AccountStatusSuspended)
    val dutyDefermentAccountClosed: DutyDefermentAccount    = dutyDefermentAccount.copy(status = AccountStatusClosed)
    val dutyDefermentAccountPending: DutyDefermentAccount   = dutyDefermentAccount.copy(status = AccountStatusPending)

    val dutyDefermentAccountSuspendedIsleOfMan: DutyDefermentAccount =
      dutyDefermentAccount.copy(status = AccountStatusSuspended, isIsleOfMan = true)

    val dutyDefermentAccountStatusOpenIsleOfMan: DutyDefermentAccount =
      dutyDefermentAccount.copy(status = AccountStatusOpen, isIsleOfMan = true)

    val dutyDefermentAccountStatusClosedIsleOfMan: DutyDefermentAccount =
      dutyDefermentAccount.copy(status = AccountStatusClosed, isIsleOfMan = true)

    val dutyDefermentAccountStatusID1: DutyDefermentAccount = dutyDefermentAccount.copy(statusId = ChangeOfLegalEntity)

    val dutyDefermentAccountStatusID2: DutyDefermentAccount =
      dutyDefermentAccount.copy(statusId = GuaranteeCancelledGuarantorsRequest)

    val dutyDefermentAccountStatusID3: DutyDefermentAccount =
      dutyDefermentAccount.copy(statusId = GuaranteeCancelledTradersRequest)

    val dutyDefermentAccountStatusID4: DutyDefermentAccount =
      dutyDefermentAccount.copy(statusId = DirectDebitMandateCancelled)

    val dutyDefermentAccountStatusID5: DutyDefermentAccount =
      dutyDefermentAccount.copy(statusId = DebitRejectedAccountClosedOrTransferred)

    val dutyDefermentAccountStatusID6: DutyDefermentAccount =
      dutyDefermentAccount.copy(statusId = DebitRejectedReferToDrawer)

    val dutyDefermentAccountStatusID7: DutyDefermentAccount = dutyDefermentAccount.copy(statusId = ReturnedMailOther)
    val dutyDefermentAccountStatusID8: DutyDefermentAccount = dutyDefermentAccount.copy(statusId = GuaranteeExceeded)
    val dutyDefermentAccountStatusID9: DutyDefermentAccount = dutyDefermentAccount.copy(statusId = AccountCancelled)

    val dutyDefermentAccountSuspendedWithStatusId4: DutyDefermentAccount =
      dutyDefermentAccount.copy(status = AccountStatusSuspended, statusId = DirectDebitMandateCancelled)

    val dutyDefermentAccountSuspendedWithStatusId7: DutyDefermentAccount =
      dutyDefermentAccount.copy(status = AccountStatusSuspended, statusId = ReturnedMailOther)

    val dutyDefermentAccountPendingZeroBalance: DutyDefermentAccount = dutyDefermentAccountPending.copy(balances =
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_999)),
        Some(BigDecimal(BALANCE_499)),
        Some(BigDecimal(BALANCE_299)),
        Some(BigDecimal(00.00))
      )
    )

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application                                      = application().build()
    implicit val appConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

    val accounts: Seq[CDSAccounts] = Seq(
      CDSAccounts(eori, None, Seq(dutyDefermentAccount, dutyDefermentAccountWithoutBalances))
    )

    val accountLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = "sessionId",
        eori,
        isNiAccount = false,
        accountNumber = dan,
        linkId = "0123456789",
        accountStatus = AccountStatusOpen,
        accountStatusId = Option(DefermentAccountAvailable),
        lastUpdated = LocalDateTime.now
      )
    )

    val model: FinancialsHomeModel = FinancialsHomeModel(
      eori,
      companyName,
      accounts = accounts,
      accountLinks = accountLinks,
      notificationMessageKeys = Seq.empty,
      xiEori = Some(emptyString)
    )

    def content(dutyDefermentAccount: DutyDefermentAccount = dutyDefermentAccount): Document = Jsoup.parse(
      app.injector
        .instanceOf[duty_deferment_account_cards]
        .apply(
          DutyDefermentAccountsViewModel(model.copy(accounts = Seq(CDSAccounts(eori, None, Seq(dutyDefermentAccount)))))
        )
        .body
    )

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

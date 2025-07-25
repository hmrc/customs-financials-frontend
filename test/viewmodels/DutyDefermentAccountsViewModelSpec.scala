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

package viewmodels

import config.AppConfig
import domain.{
  AccountLink, AccountStatusOpen, AccountStatusSuspended, CDSAccounts, DefermentAccountAvailable,
  DirectDebitMandateCancelled, DutyDefermentAccount, DutyDefermentBalance
}
import play.api.Application
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.SpecBase
import views.html.account_cards.{
  duty_deferment_account_direct_debit_setup, duty_deferment_balance_details, duty_deferment_balances
}
import views.html.components.{account_status, hidden_status}
import utils.MustMatchers

import java.time.LocalDateTime

class DutyDefermentAccountsViewModelSpec extends SpecBase with MustMatchers {

  "View model" should {

    "contain correct account section rows" when {

      "there is only one DD account" in new Setup {
        ddAccountsViewModelWithSingleAccount.accountSectionRows.head mustBe
          expectedAccountSectionRow(finHomeModel2)
      }

      "there is only one DD account and account require Direct Debit set up" in new Setup {
        ddAccountsViewModelWithSingleAccountAndDirectDebit.accountSectionRows.head mustBe
          expectedAccountSectionRowWithDirectDebitSetup(finHomeModelWithDirectDebitSetupRequired)
      }

      "there are more than one DD accounts" in new Setup {
        ddAccViewModelWithMultipleAccounts(finHomeModel4).accountSectionRows mustBe
          Seq(expectedAccountSectionRow(finHomeModel4), expectedAccountSectionRow(finHomeModel4))
      }

      "object is created using no section rows" in {
        val ddAccountsViewModelOb = new DutyDefermentAccountsViewModel("test_title", HtmlFormat.empty)

        ddAccountsViewModelOb.titleMsg mustBe "test_title"
        ddAccountsViewModelOb.inaccurateBalancesMsg mustBe HtmlFormat.empty
        ddAccountsViewModelOb.accountSectionRows mustBe empty
      }
    }
  }

  "NonDirectDebitContent.apply" should {

    "create object with correct contents with default values" in new Setup {
      val posBalance: PositiveBalanceModel =
        PositiveBalanceModel(
          pId = "duty-deferment-balance-DAN01234",
          availableBalanceValue = Some("£100"),
          availableBalanceMsg = Some(msgs("cf.available"))
        )

      val accAvailableModel: AccountAvailableModel = AccountAvailableModel(positiveBalanceValue = Some(posBalance))

      val nonDDContent: NonDirectDebitContent = NonDirectDebitContent(accountLimit = accAvailableModel)

      nonDDContent.accountLimit mustBe accAvailableModel
      nonDDContent.balances mustBe empty
      nonDDContent.viewStatements mustBe empty
      nonDDContent.topUp mustBe empty
      nonDDContent.viewStatements mustBe empty
    }
  }

  "ContentRowModel" should {
    "create object with correct contents with default values" in {

      val contentRowModel: ContentRowModel = ContentRowModel()

      contentRowModel.directDebitSetupComponent mustBe empty
      contentRowModel.nonDirectDebitContent mustBe empty
    }
  }

  private def expectedAccountSectionRow(
    finHomeModel: FinancialsHomeModel
  )(implicit msgs: Messages, appConfig: AppConfig): DutyDefermentAccountRowModel = {
    val headerRowModel =
      HeaderRowModel(
        "dan-DAN01234",
        s"${msgs("cf.account")} DAN01234",
        new hidden_status().apply(AccountStatusOpen),
        new account_status().apply(AccountStatusOpen, "duty-deferment")
      )

    val posBalance =
      PositiveBalanceModel(
        pId = "duty-deferment-balance-DAN01234",
        availableBalanceValue = Some("£100"),
        availableBalanceMsg = Some(msgs("cf.available"))
      )

    val accAvailableModel = AccountAvailableModel(positiveBalanceValue = Some(posBalance))

    val nonDDContent = NonDirectDebitContent(
      accountLimit = accAvailableModel,
      balances = Some(
        new duty_deferment_balances(new duty_deferment_balance_details)
          .apply(finHomeModel.dutyDefermentAccounts.head, finHomeModel)
      ),
      viewStatements = Some(
        FooterLinkModel(
          id = "duty-deferment-account-DAN01234",
          href =
            finHomeModel.dutyDefermentAccountDetailsLinks()(appConfig)(("test_eori", "DAN01234"): (String, String)),
          displayValue = msgs("cf.accounts.viewStatements"),
          hiddenMsg = msgs("cf.accounts.label.dan", "DAN01234")
        )
      ),
      paymentDetails = Some(
        FooterLinkModel(
          id = "payment-details-DAN01234",
          href =
            finHomeModel.dutyDefermentAccountDDSetupLinks()(appConfig)(("test_eori", "DAN01234"): (String, String)),
          displayValue = msgs("cf.accounts.contact.details"),
          hiddenMsg = msgs("cf.accounts.label.contact.details", "DAN01234")
        )
      ),
      topUp = Some(
        FooterLinkModel(
          id = emptyString,
          classValue = "govuk-link",
          href = appConfig.dutyDefermentTopUpLink,
          displayValue = msgs("cf.accounts.topUp"),
          hiddenMsg = msgs("cf.accounts.label.topUp", "DAN01234")
        )
      ),
      pendingAccountGuidance = None
    )

    val contentRowModel = ContentRowModel(directDebitSetupComponent = None, nonDirectDebitContent = Some(nonDDContent))

    DutyDefermentAccountRowModel(headerRowModel, contentRowModel)
  }

  private def expectedAccountSectionRowWithDirectDebitSetup(
    finHomeModel: FinancialsHomeModel
  )(implicit msgs: Messages, appConfig: AppConfig): DutyDefermentAccountRowModel = {
    val headerRowModel =
      HeaderRowModel(
        "dan-DAN01234",
        s"${msgs("cf.account")} DAN01234",
        new hidden_status().apply(AccountStatusSuspended),
        new account_status().apply(AccountStatusSuspended, "duty-deferment")
      )

    val ddSetupComponent = Some(
      new duty_deferment_account_direct_debit_setup()
        .apply(finHomeModel.dutyDefermentAccounts.head, finHomeModel.dutyDefermentAccountDDSetupLinks())
    )

    val contentRowModel = ContentRowModel(directDebitSetupComponent = ddSetupComponent, nonDirectDebitContent = None)

    DutyDefermentAccountRowModel(headerRowModel, contentRowModel)
  }

  trait Setup {
    val eoriNumber                  = "test_eori"
    val eori1                       = "test_eori1"
    val eori2                       = "test_eori2"
    val companyName: Option[String] = Some("Company Name 1")

    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    val date: LocalDateTime = LocalDateTime.now()
    val sessionId           = "test_session_id"
    val linkId              = "test_link_id"

    val amountLimit: BigDecimal = BigDecimal(100.00)

    implicit val app: Application     = application().build()
    implicit val msgs: Messages       = messages(app)
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val cdsAccounts1: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eoriNumber,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      ),
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan2,
            eoriNumber,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val cdsAccounts2: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eoriNumber,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val cdsAccounts3: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eoriNumber,
            isNiAccount = false,
            AccountStatusSuspended,
            DirectDebitMandateCancelled,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val cdsAccounts4: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eoriNumber,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      ),
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eoriNumber,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit),
              Some(amountLimit)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val accountLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = sessionId,
        eori = eoriNumber,
        isNiAccount = false,
        accountNumber = dan1,
        accountStatus = AccountStatusOpen,
        accountStatusId = Some(DefermentAccountAvailable),
        linkId = linkId,
        lastUpdated = date
      ),
      AccountLink(
        sessionId = sessionId,
        eori = eoriNumber,
        isNiAccount = false,
        accountNumber = dan2,
        accountStatus = AccountStatusOpen,
        accountStatusId = Some(DefermentAccountAvailable),
        linkId = linkId,
        lastUpdated = date
      )
    )

    val finHomeModel1: FinancialsHomeModel =
      FinancialsHomeModel(eoriNumber, companyName, cdsAccounts1, notificationMessageKeys = List(), accountLinks, None)

    val finHomeModel2: FinancialsHomeModel =
      FinancialsHomeModel(eoriNumber, companyName, cdsAccounts2, notificationMessageKeys = List(), accountLinks, None)

    val finHomeModel4: FinancialsHomeModel =
      FinancialsHomeModel(eoriNumber, companyName, cdsAccounts4, notificationMessageKeys = List(), accountLinks, None)

    val finHomeModelWithDirectDebitSetupRequired: FinancialsHomeModel =
      FinancialsHomeModel(eoriNumber, companyName, cdsAccounts3, notificationMessageKeys = List(), accountLinks, None)

    val ddAccountsViewModelWithSingleAccount: DutyDefermentAccountsViewModel =
      DutyDefermentAccountsViewModel(finHomeModel2)

    val ddAccountsViewModelWithSingleAccountAndDirectDebit: DutyDefermentAccountsViewModel =
      DutyDefermentAccountsViewModel(finHomeModelWithDirectDebitSetupRequired)

    def ddAccViewModelWithMultipleAccounts(model: FinancialsHomeModel = finHomeModel1): DutyDefermentAccountsViewModel =
      DutyDefermentAccountsViewModel(model)
  }
}

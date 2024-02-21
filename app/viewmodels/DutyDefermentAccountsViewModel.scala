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
import domain.{AccountStatusClosed, AccountStatusPending, AccountStatusSuspended, DutyDefermentAccount}
import play.api.i18n.{Lang, Messages}
import play.twirl.api.HtmlFormat
import utils.Utils.emptyString
import views.helpers.Formatters
import views.html.account_cards._
import views.html.components.{account_status, hidden_status}

import scala.collection.immutable.Seq

case class PositiveBalanceModel(pId: String,
                                cyAvailableBalanceValue: Option[String] = None,
                                cyAvailableBalanceMsg: Option[String] = None,
                                cyAvailableBalancePreMsg: Option[String] = None,
                                cyAvailableBalancePostMsg: Option[String] = None,
                                availableBalanceValue: Option[String] = None,
                                availableBalanceMsg: Option[String] = None)

case class FooterLinkModel(id: String = emptyString,
                           classValue: String = "govuk-link govuk-!-margin-right-2",
                           href: String,
                           displayValue: String,
                           hiddenMsg: String)

case class AccountAvailableModel(positiveBalanceValue: Option[PositiveBalanceModel] = None,
                                 negativeBalanceValue: Option[String] = None)

case class NonDirectDebitContent(accountLimit: AccountAvailableModel,
                                 balances: Option[HtmlFormat.Appendable] = None,
                                 viewStatements: Option[FooterLinkModel] = None,
                                 paymentDetails: Option[FooterLinkModel] = None,
                                 topUp: Option[FooterLinkModel] = None,
                                 pendingAccountGuidance: Option[HtmlFormat.Appendable] = None)

case class HeaderRowModel(id: String,
                          accountHeadingMsg: String,
                          hiddenStatus: HtmlFormat.Appendable,
                          accountStatus: HtmlFormat.Appendable)

case class ContentRowModel(directDebitSetupComponent: Option[HtmlFormat.Appendable] = None,
                           nonDirectDebitContent: Option[NonDirectDebitContent] = None)

case class DutyDefermentAccountRowModel(headerRow: HeaderRowModel,
                                        contentRow: ContentRowModel)

case class DutyDefermentAccountsViewModel(titleMsg: String,
                                          inaccurateBalancesMsg: duty_deferment_inaccurate_balances_message,
                                          accountSectionRows: Seq[DutyDefermentAccountRowModel] = Nil)

object DutyDefermentAccountsViewModel {
  def apply(finHomeModel: FinancialsHomeModel)(implicit messages: Messages,
                                               appConfig: AppConfig): DutyDefermentAccountsViewModel = {

    DutyDefermentAccountsViewModel(
      titleMsg(finHomeModel.dutyDefermentAccounts),
      new duty_deferment_inaccurate_balances_message(),
      populateAccountSectionRows(finHomeModel)
    )
  }

  private def titleMsg(ddAccounts: Seq[DutyDefermentAccount])(implicit messages: Messages): String = {
    if (ddAccounts.size > 1) {
      messages("cf.customs-financials-home.duty-deferment.title2")
    } else {
      messages("cf.customs-financials-home.duty-deferment.title")
    }
  }

  private def populateAccountSectionRows(finHomeModel: FinancialsHomeModel)
                                        (implicit messages: Messages,
                                         appConfig: AppConfig): Seq[DutyDefermentAccountRowModel] = {
    finHomeModel.dutyDefermentAccounts.map {
      ddAccount => populateAccountRowModel(ddAccount, finHomeModel)
    }
  }

  private def populateAccountRowModel(account: DutyDefermentAccount,
                                      finHomeModel: FinancialsHomeModel)
                                     (implicit messages: Messages, appConfig: AppConfig): DutyDefermentAccountRowModel = {
    DutyDefermentAccountRowModel(
      headerRow = headerRow(account, finHomeModel),
      contentRow = contentRow(account, finHomeModel)
    )
  }

  private def headerRow(account: DutyDefermentAccount,
                        finHomeModel: FinancialsHomeModel)(implicit messages: Messages): HeaderRowModel = {
    val accountHeadingMsg =
      if (finHomeModel.isNiAccountIndicator((account.owner, account.number): (String, String))) {
        s"${messages("cf.NiAccount")} ${account.number}"
      } else {
        s"${messages("cf.account")} ${account.number}"
      }

    val hiddenStatus: HtmlFormat.Appendable = new hidden_status().apply(account.status)

    val accountStatus: HtmlFormat.Appendable = new account_status().apply(account.status, "duty-deferment")

    HeaderRowModel(
      id = s"dan-${account.number}",
      accountHeadingMsg = accountHeadingMsg,
      hiddenStatus = hiddenStatus,
      accountStatus = accountStatus)
  }

  private def contentRow(account: DutyDefermentAccount,
                         finHomeModel: FinancialsHomeModel)
                        (implicit messages: Messages, appConfig: AppConfig): ContentRowModel = {

    val directDebitSetupComponent: Option[HtmlFormat.Appendable] =
      if (account.status == AccountStatusSuspended && account.isDirectDebitSetupRequired) {
        Some(new duty_deferment_account_direct_debit_setup()(account, finHomeModel.dutyDefermentAccountDDSetupLinks()))
      } else {
        None
      }

    ContentRowModel(
      directDebitSetupComponent = directDebitSetupComponent,
      nonDirectDebitContent =
        if (directDebitSetupComponent.isEmpty) {
          nonDirectDebitContent(account, finHomeModel)
        } else {
          None
        }
    )
  }

  private def nonDirectDebitContent(account: DutyDefermentAccount,
                                    finHomeModel: FinancialsHomeModel)
                                   (implicit messages: Messages, appConfig: AppConfig): Option[NonDirectDebitContent] = {

    val balances = if (account.status != AccountStatusPending) {
      Some(new duty_deferment_balances(new duty_deferment_balance_details)(account, finHomeModel))
    } else {
      None
    }

    val pendingAccountGuidance = if (account.status == AccountStatusPending) {
      Some(new duty_deferment_account_pending().apply())
    } else {
      None
    }

    Some(
      NonDirectDebitContent(
        accountLimit = accountLimit(account),
        balances = balances,
        viewStatements = viewStatements(account, finHomeModel),
        paymentDetails = paymentDetails(account, finHomeModel),
        topUp = topUp(account),
        pendingAccountGuidance = pendingAccountGuidance
      ))
  }

  private def accountLimit(account: DutyDefermentAccount)
                          (implicit messages: Messages): AccountAvailableModel = {

    val positiveBalance: Option[PositiveBalanceModel] = positiveBalanceValue(account)

    val negativeBalanceValue =
      if (positiveBalance.isEmpty) {
        Some(Formatters.formatCurrencyAmount(account.balances.availableBalance.abs))
      } else {
        None
      }

    AccountAvailableModel(
      positiveBalanceValue = positiveBalance,
      negativeBalanceValue = negativeBalanceValue)
  }

  private def viewStatements(account: DutyDefermentAccount,
                             finHomeModel: FinancialsHomeModel)
                            (implicit messages: Messages, appConfig: AppConfig): Option[FooterLinkModel] = {
    if (account.status != AccountStatusPending) {
      Some(
        FooterLinkModel(
          id = s"duty-deferment-account-${account.number}",
          href = finHomeModel.dutyDefermentAccountDetailsLinks()(appConfig)((account.owner, account.number): (String, String)),
          displayValue = messages("cf.accounts.viewStatements"),
          hiddenMsg = messages("cf.accounts.label.dan", account.number))
      )
    } else {
      None
    }
  }

  private def paymentDetails(account: DutyDefermentAccount,
                             finHomeModel: FinancialsHomeModel)
                            (implicit messages: Messages, appConfig: AppConfig): Option[FooterLinkModel] =
    if (!List(AccountStatusPending, AccountStatusClosed).contains(account.status)) {
      Some(
        FooterLinkModel(
          id = s"payment-details-${account.number}",
          href = finHomeModel.dutyDefermentAccountDDSetupLinks()(appConfig)((account.owner, account.number): (String, String)),
          displayValue = messages("cf.accounts.contact.details"),
          hiddenMsg = messages("cf.accounts.label.contact.details", account.number))
      )
    } else {
      None
    }

  private def topUp(account: DutyDefermentAccount)
                   (implicit messages: Messages, appConfig: AppConfig): Option[FooterLinkModel] =
    if (account.status != AccountStatusPending) {
      Some(
        FooterLinkModel(
          classValue = "govuk-link",
          href = appConfig.dutyDefermentTopUpLink,
          displayValue = messages("cf.accounts.topUp"),
          hiddenMsg = messages("cf.accounts.label.topUp", account.number))
      )
    } else {
      None
    }

  private def positiveBalanceValue(account: DutyDefermentAccount)
                                  (implicit messages: Messages): Option[PositiveBalanceModel] = {
    if (account.balances.availableBalance >= 0) {
      if (messages.lang == Lang("cy")) {

        if (account.status == AccountStatusPending) {

          val cyAvailableBalancePreMsg: Option[String] = if (account.balances.availableBalance > 0) {
            Some(messages("cf.pending.available.pre"))
          } else { None }

          val cyAvailableBalancePostMsg: Option[String] = if (account.balances.availableBalance > 0) {
            Some(messages("cf.pending.available.post"))
          } else { None }

          val cyAvailableBalanceValue = Some(Formatters.formatCurrencyAmount(account.balances.availableBalance))

          Some(
            PositiveBalanceModel(
              pId = s"duty-deferment-balance-${account.number}",
              cyAvailableBalancePreMsg = cyAvailableBalancePreMsg,
              cyAvailableBalancePostMsg = cyAvailableBalancePostMsg,
              cyAvailableBalanceValue = cyAvailableBalanceValue))

        } else {
          val cyAvailableBalanceValue = Some(Formatters.formatCurrencyAmount(account.balances.availableBalance))
          val cyAvailableBalanceMsg = if (account.balances.availableBalance > 0) {
            Some(messages("cf.available"))
          } else { None }

          Some(PositiveBalanceModel(
            pId = s"duty-deferment-balance-${account.number}",
            cyAvailableBalanceValue = cyAvailableBalanceValue,
            cyAvailableBalanceMsg = cyAvailableBalanceMsg))
        }
      } else {

        val availBalanceValue = Some(Formatters.formatCurrencyAmount(account.balances.availableBalance))
        val availBalanceMsg =
          Some(if (account.status == AccountStatusPending && account.balances.availableBalance > 0) {
            messages("cf.pending.available")
          } else {
            if (account.balances.availableBalance > 0) messages("cf.available") else emptyString
          })

        Some(
          PositiveBalanceModel(pId = s"duty-deferment-balance-${account.number}",
            availableBalanceValue = availBalanceValue, availableBalanceMsg = availBalanceMsg))
      }

    } else { None }
  }
}

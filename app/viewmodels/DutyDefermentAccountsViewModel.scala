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
import domain.{DutyDefermentAccount, EORI}
import play.api.i18n.Messages
import utils.Utils.emptyString
import views.html.account_cards.{duty_deferment_account_direct_debit_setup, duty_deferment_account_pending, duty_deferment_balances, duty_deferment_inaccurate_balances_message}
import views.html.components.{account_status, hidden_status}

import scala.collection.immutable.Seq

case class PositiveBalanceModel(cyAvailableBalanceValue:Option[String] = None,
                                cyAvailableBalanceMsg: Option[String] = None,
                                cyAvailableBalancePreMsg: Option[String] = None,
                                cyAvailableBalancePostMsg: Option[String] = None,
                                availableBalanceValue: Option[String] = None,
                                availableBalanceMsg: Option[String] = None)

case class FooterLinkModel(id: String,
                           classValue: String = "govuk-link govuk-!-margin-right-2",
                           href: String,
                           displayValue: String,
                           hiddenMsg: String)

case class AccountAvailableModel(positiveBalanceValue: Option[PositiveBalanceModel] = None,
                                  negativeBalanceValue: Option[String] = None)

case class NonDirectDebitContent(accountLimit: AccountAvailableModel,
                                 balances: Option[duty_deferment_balances] = None,
                                 viewStatements: Option[FooterLinkModel] = None,
                                 paymentDetails: Option[FooterLinkModel] = None,
                                 topUp: Option[FooterLinkModel] = None,
                                 pendingAccountGuidance: Option[duty_deferment_account_pending] = None)

case class HeaderRowModel(accountHeadingMsg: String,
                          hiddenStatus: hidden_status,
                          accountStatus: account_status)

case class ContentRowModel(directDebitSetupComponent: Option[duty_deferment_account_direct_debit_setup] = None,
                           nonDirectDebitContent: Option[NonDirectDebitContent] = None)

case class DutyDefermentAccountRowModel(headerRow: HeaderRowModel,
                                        contentRow: ContentRowModel)

case class DutyDefermentAccountsViewModel(titleMsg: String,
                                          inaccurateBalancesMsg:duty_deferment_inaccurate_balances_message,
                                          accountSectionRows: Seq[DutyDefermentAccountRowModel] = Nil)

object DutyDefermentAccountsViewModel {
  def apply(isNiAccountIndicator:Map[(EORI, String), Boolean],
            dutyDefermentAccountDDSetupLinks:Map[(String, String), String],
            ddAccounts: Seq[DutyDefermentAccount])(implicit messages: Messages,
                                                   appConfig: AppConfig): DutyDefermentAccountsViewModel = {

    DutyDefermentAccountsViewModel(emptyString, new duty_deferment_inaccurate_balances_message(), Seq())
  }
}

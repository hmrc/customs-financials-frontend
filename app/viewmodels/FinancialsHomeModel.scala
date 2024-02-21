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
import domain.CDSAccounts.{filterCashAccounts, filterDutyDefermentAccounts, filterGuaranteeAccounts}
import domain._

case class FinancialsHomeModel(eori: EORI,
                               companyName: Option[String],
                               accounts: Seq[CDSAccounts],
                               notificationMessageKeys: Seq[String],
                               accountLinks: Seq[AccountLink],
                               xiEori: Option[String] = None) {
  private val allMyAccounts: Seq[CDSAccount] = accounts.flatMap(_.myAccounts)
  val dutyDefermentAccounts: Seq[DutyDefermentAccount] = filterDutyDefermentAccounts(allMyAccounts)
  val guaranteeAccountViewModels: Seq[GeneralGuaranteeAccount] = filterGuaranteeAccounts(allMyAccounts)
  val cashAccounts: Seq[CashAccount] = filterCashAccounts(allMyAccounts)
  val isAgent: Boolean = accounts.exists(_.isAgent)

  def dutyDefermentAccountDetailsLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = accountLinks.map { accountLink =>
    (accountLink.eori, accountLink.accountNumber) -> appConfig.accountUrl(accountLink.linkId)
  }.toMap

  def dutyDefermentAccountDDSetupLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = accountLinks.map { accountLink =>
    (accountLink.eori, accountLink.accountNumber) -> appConfig.directDebitUrl(accountLink.linkId)
  }.toMap

  def dutyDefermentContactDetailsLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = {
    accountLinks.map { accountLink =>
      (accountLink.eori, accountLink.accountNumber) -> appConfig.contactDetailsUrl(accountLink.linkId)
    }.toMap
  }

  def isNiAccountIndicator: Map[(EORI, String), Boolean] = {
    accountLinks.map { accountLink =>
      (accountLink.eori, accountLink.accountNumber) -> accountLink.isNiAccount
    }.toMap
  }

}

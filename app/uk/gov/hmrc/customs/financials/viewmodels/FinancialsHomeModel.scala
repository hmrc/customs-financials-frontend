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

package uk.gov.hmrc.customs.financials.viewmodels

import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.controllers.routes
import uk.gov.hmrc.customs.financials.domain
import uk.gov.hmrc.customs.financials.domain.CDSAccounts.{filterCashAccounts, filterDutyDefermentAccounts, filterGuaranteeAccounts}
import uk.gov.hmrc.customs.financials.domain._

case class FinancialsHomeModel(eori: EORI,
                               companyName: String,
                               accounts: Seq[CDSAccounts],
                               notificationMessageKeys: Seq[String],
                               accountLinks: Seq[AccountLink]) {
  val allMyAccounts: Seq[CDSAccount] = accounts.flatMap(_.myAccounts)
  val dutyDefermentAccounts: Seq[domain.DutyDefermentAccount] = filterDutyDefermentAccounts(allMyAccounts)
  val guaranteeAccountViewModels: Seq[GeneralGuaranteeAccountViewModel] = filterGuaranteeAccounts(allMyAccounts).map(GeneralGuaranteeAccountViewModel(_))
  val cashAccounts: Seq[CashAccount] = filterCashAccounts(allMyAccounts)
  val isAgent: Boolean = accounts.exists(_.isAgent)
  val hasCashAccounts: Boolean = cashAccounts.nonEmpty
  val hasDutyDefermentAccounts: Boolean = dutyDefermentAccounts.nonEmpty
  val hasGuaranteeAccounts: Boolean = guaranteeAccountViewModels.nonEmpty

  def dutyDefermentAccountDetailsLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = accountLinks.map { accountLink =>
    (accountLink.eori, accountLink.accountNumber) -> appConfig.accountUrl(accountLink.linkId)
  }.toMap

  def dutyDefermentAccountDDSetupLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = accountLinks.map { accountLink =>
    (accountLink.eori, accountLink.accountNumber) -> appConfig.directDebitUrl(accountLink.linkId)
  }.toMap

  def accountStatus(accountNumber: String): String = {
    dutyDefermentAccounts
      .find(_.number == accountNumber)
      .map(_.status.name)
      .getOrElse("")
  }

  def dutyDefermentContactDetailsLinks()(implicit appConfig: AppConfig): Map[(String, String), String] = {
    accountLinks.map { accountLink =>
      (accountLink.eori, accountLink.accountNumber) -> appConfig.contactDetailsUrl(accountLink.linkId)
    }.toMap
  }
}

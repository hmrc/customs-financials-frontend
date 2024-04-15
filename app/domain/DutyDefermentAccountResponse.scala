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

package domain

import play.api.libs.json.{Json, Reads}

case class DutyDefermentAccountResponse(account: AccountResponse,
                                        isIomAccount: Option[Boolean] = Some(false),
                                        isNiAccount: Option[Boolean] = Some(false),
                                        limits: Option[Limits], balances: Option[DefermentBalancesResponse]) {

  def toDomain: domain.DutyDefermentAccount = {
    val balance = domain.DutyDefermentBalance(
      limits.map(limit => BigDecimal(limit.periodGuaranteeLimit)),
      limits.map(limit => BigDecimal(limit.periodAccountLimit)),
      balances.map(balance => BigDecimal(balance.periodAvailableGuaranteeBalance)),
      balances.map(balance => BigDecimal(balance.periodAvailableAccountBalance)))

    val isIomFlag = if (account.isleOfManFlag.isDefined) {
      account.isleOfManFlag
    } else {
      isIomAccount
    }

    domain.DutyDefermentAccount(account.number, account.owner,
      isNiAccount.getOrElse(false), account.accountStatus.getOrElse(AccountStatusOpen),
      account.accountStatusID.getOrElse(DefermentAccountAvailable), balance,
      account.viewBalanceIsGranted, isIomFlag.getOrElse(false))
  }
}

object DutyDefermentAccountResponse {
  implicit val reads: Reads[DutyDefermentAccountResponse] = Json.reads[DutyDefermentAccountResponse]
}

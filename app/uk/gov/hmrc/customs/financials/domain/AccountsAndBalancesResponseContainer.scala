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

package uk.gov.hmrc.customs.financials.domain

import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.customs.financials.domain

case class AccountsAndBalancesResponseContainer(accountsAndBalancesResponse: AccountsAndBalancesResponse) {
  def toCdsAccounts(eori:String): domain.CDSAccounts = {
    val details: AccountResponseDetail = this.accountsAndBalancesResponse.responseDetail
    val dutyDefermentAccounts: Seq[domain.DutyDefermentAccount] = details.dutyDefermentAccount.fold(Seq.empty[domain.DutyDefermentAccount])(_.map(_.toDomain()))
    val generalGuaranteeAccounts: Seq[domain.GeneralGuaranteeAccount] = details.generalGuaranteeAccount.fold(Seq.empty[domain.GeneralGuaranteeAccount])(_.map(_.toDomain()))
    val cashAccounts: Seq[CashAccount] = details.cdsCashAccount.fold(Seq.empty[CashAccount])(_.map(_.toDomain()))

    domain.CDSAccounts(eori, dutyDefermentAccounts ++ generalGuaranteeAccounts ++ cashAccounts)
  }
}

object AccountsAndBalancesResponseContainer {
  implicit val reads: Reads[AccountsAndBalancesResponseContainer] = Json.reads[AccountsAndBalancesResponseContainer]
}

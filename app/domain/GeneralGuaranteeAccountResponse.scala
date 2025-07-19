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

import play.api.libs.json.{Json, OWrites, Reads}

case class GeneralGuaranteeAccountResponse(
  account: AccountResponse,
  guaranteeLimit: Option[String],
  availableGuaranteeBalance: Option[String]
) {
  def toDomain: domain.GeneralGuaranteeAccount = {
    val balance = (guaranteeLimit, availableGuaranteeBalance) match {
      case (Some(limit), Some(guarantee)) => Some(GeneralGuaranteeBalance(BigDecimal(limit), BigDecimal(guarantee)))
      case (None, Some(guarantee))        => Some(GeneralGuaranteeBalance(BigDecimal(0), BigDecimal(guarantee)))
      case _                              => None
    }

    domain.GeneralGuaranteeAccount(
      account.number,
      account.owner,
      account.accountStatus.getOrElse(AccountStatusOpen),
      account.accountStatusID.getOrElse(DefermentAccountAvailable),
      balance
    )
  }
}

object GeneralGuaranteeAccountResponse {
  implicit val reads: Reads[GeneralGuaranteeAccountResponse]         = Json.reads[GeneralGuaranteeAccountResponse]
  implicit val ggAccWrites: OWrites[GeneralGuaranteeAccountResponse] = Json.writes[GeneralGuaranteeAccountResponse]
}

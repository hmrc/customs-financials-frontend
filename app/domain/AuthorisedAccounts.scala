/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

sealed trait AuthorisedAccounts

case class AuthorisedGeneralGuaranteeAccount(account: Account,
                                             availableGuaranteeBalance: Option[Double]) extends AuthorisedAccounts

object AuthorisedGeneralGuaranteeAccount {
  implicit val format: OFormat[AuthorisedGeneralGuaranteeAccount] = Json.format[AuthorisedGeneralGuaranteeAccount]
}

case class AuthorisedDutyDefermentAccount(account: Account,
                                          balances: Option[AuthorisedBalances]) extends AuthorisedAccounts

object AuthorisedDutyDefermentAccount {
  implicit val format: OFormat[AuthorisedDutyDefermentAccount] = Json.format[AuthorisedDutyDefermentAccount]
}

case class AuthorisedCashAccount(account: Account,
                                 availableAccountBalance: Option[Double]) extends AuthorisedAccounts

object AuthorisedCashAccount {
  implicit val format: OFormat[AuthorisedCashAccount] = Json.format[AuthorisedCashAccount]
}

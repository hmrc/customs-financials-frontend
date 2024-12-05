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

import domain.DutyPaymentMethod.CDS
import utils.SpecBase
import utils.MustMatchers

class AccountResponseDetailSpec extends SpecBase with MustMatchers {

  "totalNumberOfAccounts" should {

    "return correct no of accounts" when {
      "dutyDefermentAccount, generalGuaranteeAccount and cdsCashAccount have accounts" in new Setup {
        accResDetailObWithAccount.totalNumberOfAccounts mustBe 3
      }

      "dutyDefermentAccount, generalGuaranteeAccount and cdsCashAccount have no accounts" in new Setup {
        accResDetailObWithNoAccount.totalNumberOfAccounts mustBe 0
      }
    }
  }

  trait Setup {
    val eori = "test_eori"
    val referenceDate = "2023-10-12"
    val accNumber = "12345678"

    val accRes: AccountResponse = AccountResponse(accNumber, "dd", CDS, None, None, viewBalanceIsGranted = true)

    val ddAccRes: DutyDefermentAccountResponse =
      DutyDefermentAccountResponse(account = accRes, limits = None, balances = None)

    val ggAccRes: GeneralGuaranteeAccountResponse = GeneralGuaranteeAccountResponse(accRes, None, None)

    val cashAccRes: CdsCashAccountResponse = CdsCashAccountResponse(account = accRes, availableAccountBalance = None)

    val accResDetailObWithAccount: AccountResponseDetail =
      AccountResponseDetail(
        Some(eori),
        Some(referenceDate),
        Some(Seq(ddAccRes)),
        Some(Seq(ggAccRes)),
        Some(Seq(cashAccRes))
      )

    val accResDetailObWithNoAccount: AccountResponseDetail =
      AccountResponseDetail(Some(eori), Some(referenceDate), None, None, None)
  }
}

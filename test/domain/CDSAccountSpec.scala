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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import utils.SpecBase
import views.helpers.Formatters

class CDSAccountSpec extends SpecBase {

  "DutyDefermentAccount.displayBalances" should {

    "return correct output" when {
      "periodAccountLimit is greater than 0 and periodGuaranteeLimit is 0 " in new Setup {
        ddAccount.displayBalances(
          Some(BigDecimal(amountLimit)),
          Some(BigDecimal(amountLimitZero)),
          Some(BigDecimal(amountLimit))) mustBe Some(DutyDefermentDisplayBalance(
          Some(Formatters.formatCurrencyAmount(amountLimit)),
          None,
          None)
        )
      }

      "periodAccountLimit is 0 and periodGuaranteeLimit is greater than 0 " in new Setup {
        ddAccount.displayBalances(
          Some(BigDecimal(amountLimitZero)),
          Some(BigDecimal(amountLimit)),
          Some(BigDecimal(amountLimit))) mustBe Some(DutyDefermentDisplayBalance(
          None,
          Some(Formatters.formatCurrencyAmount(amountLimit)),
          None)
        )
      }
    }

    "periodAccountLimit, periodGuaranteeLimit and periodGuaranteeLimitRemaining are None" in new Setup {
      ddAccount.displayBalances(None, None, None) mustBe empty
    }
  }

  "DutyDefermentAccount.compare" should {
    "order DutyDefermentAccount correctly" in new Setup {
      private val ddAccount1 = ddAccount
      private val ddAccount2 = ddAccount.copy(number = "123456789")

      List(ddAccount2, ddAccount1).sorted mustBe List(ddAccount1, ddAccount2)
    }
  }

  trait Setup {
    val accNum = "12345678"
    val owner = "test"

    val amountLimit = 10
    val amountLimitZero = 0

    val ddBalance: DutyDefermentBalance = DutyDefermentBalance(Some(BigDecimal(amountLimit)), None, None, None)

    val ddAccount: DutyDefermentAccount = DutyDefermentAccount(accNum,
      owner,
      isNiAccount = true,
      AccountStatusOpen,
      DefermentAccountAvailable,
      ddBalance,
      viewBalanceIsGranted = true,
      isIsleOfMan = false)
  }
}

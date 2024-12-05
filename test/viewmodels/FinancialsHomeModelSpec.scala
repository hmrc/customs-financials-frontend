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

import domain.{
  AccountLink,
  AccountStatusClosed,
  AccountStatusOpen,
  CDSAccounts,
  DefermentAccountAvailable,
  DutyDefermentAccount,
  DutyDefermentBalance
}
import utils.SpecBase

import java.time.LocalDateTime
import utils.MustMatchers

class FinancialsHomeModelSpec extends SpecBase with MustMatchers {

  "dutyDefermentAccounts" should {

    "return correct accounts size" when {

      "Duty Deferment accounts are present" in new Setup {
        homeModel.dutyDefermentAccounts.size mustBe 1
      }

      "there is no Duty Deferment account" in new Setup {
        homeModelWithNoAccounts.dutyDefermentAccounts mustBe empty
      }
    }
  }

  trait Setup {
    val eori1 = "test_eori_1"
    val eori2 = "test_eori_2"
    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    val periodGuaranteeLimit: BigDecimal = BigDecimal(100.0)
    val periodAccountLimit: BigDecimal = BigDecimal(100.0)
    val periodAvailableGuaranteeBalance: BigDecimal = BigDecimal(100.0)
    val periodAvailableAccountBalance: BigDecimal = BigDecimal(100.0)

    val ddAccount1WithEori1: DutyDefermentAccount =
      DutyDefermentAccount(
        dan1,
        eori1,
        isNiAccount = false,
        AccountStatusClosed,
        DefermentAccountAvailable,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false
      )

    val ddAccount1WithEori2: DutyDefermentAccount =
      DutyDefermentAccount(
        dan2,
        eori2,
        isNiAccount = false,
        AccountStatusClosed,
        DefermentAccountAvailable,
        DutyDefermentBalance(
          Some(periodGuaranteeLimit),
          Some(periodAccountLimit),
          Some(periodAvailableGuaranteeBalance),
          Some(periodAvailableAccountBalance)
        ),
        viewBalanceIsGranted = true,
        isIsleOfMan = false
      )

    val accounts: Seq[CDSAccounts] = Seq(CDSAccounts(eori1, None, Seq(ddAccount1WithEori1, ddAccount1WithEori2)))

    val accountLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = "sessionId",
        eori1,
        isNiAccount = false,
        accountNumber = dan1,
        linkId = "linkId",
        accountStatus = AccountStatusOpen,
        accountStatusId = Option(DefermentAccountAvailable),
        lastUpdated = LocalDateTime.now()
      )
    )

    val homeModel: FinancialsHomeModel = FinancialsHomeModel(eori1, None, accounts, Nil, accountLinks)
    val homeModelWithNoAccounts: FinancialsHomeModel = FinancialsHomeModel(eori1, None, Nil, Nil, accountLinks)
  }
}

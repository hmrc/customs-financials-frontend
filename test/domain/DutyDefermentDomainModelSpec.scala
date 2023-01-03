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

import utils.SpecBase
import domain.{DefermentBalancesResponse => Bal, DutyDefermentAccountResponse => DDA}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class DutyDefermentDomainModelSpec extends SpecBase    {

  "DutyDefermentDomainModel" should {

    "correctly form the domain model when status is none to AccountStatusOpen" in {
      val statusList = List(
        Some(AccountStatusOpen),
        Some(AccountStatusSuspended),
        Some(AccountStatusClosed),
        None
      )
      val expectedDDBalance = DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), Some(BigDecimal(50)), Some(BigDecimal(20)))
      val expectedDDA = domain.DutyDefermentAccount("1231231231", "EORI12345678", AccountStatusOpen, DefermentAccountAvailable, expectedDDBalance, viewBalanceIsGranted = false, isIsleOfMan = false)
      val account = AccountResponse("1231231231", "", "EORI12345678",accountStatus = None, None, viewBalanceIsGranted = false, None)
      val dda = DDA(account, Some(Limits("200", "100")), Some(Bal("50", "20")))

      statusList.foreach { case(status) => {
        val dd = dda.copy(account = account.copy(accountStatus = status))
        dd.toDomain() mustBe expectedDDA.copy(status = status.getOrElse(AccountStatusOpen))
        }
      }
    }

    "correctly set DutyDefermentAccount domain model based isleOfManFlag" in {
      val iomList = List(
        Some(true),
        Some(false),
        None
      )
      val account = AccountResponse("1231231231", "", "EORI12345678",accountStatus = None, isleOfManFlag = None, accountStatusID = None, viewBalanceIsGranted = false)
      val dda = DDA(account, Some(Limits("200", "100")), Some(Bal("50", "20")))
      iomList.foreach { case(iom) => {
        val dd = dda.copy(account = account.copy(isleOfManFlag = iom))
        dd.toDomain().isIsleOfMan mustBe iom.getOrElse(false)
        }
      }
    }

  }
}

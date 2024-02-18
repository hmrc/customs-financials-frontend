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

import domain.{AccountStatusOpen, CDSAccounts, DefermentAccountAvailable, DutyDefermentAccount, DutyDefermentBalance, AccountLink}
import utils.SpecBase

import scala.util.Random

class DutyDefermentAccountsViewModelSpec extends SpecBase {

  "View model" should {

    "contain correct title and inaccurate balances msg" when {

      "there is only one DD account" in new Setup {

      }

      "there are more than one DD account" in new Setup {

      }
    }

    "contain correct account section rows" in new Setup {

    }
  }

  trait Setup {

    val eoriNumber = "test_eori"
    val eori1 = "test_eori1"
    val eori2 = "test_eori2"
    val companyName = Some("Company Name 1")

    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    def randomFloat: Float = Random.nextFloat()

    def randomBigDecimal: BigDecimal = BigDecimal(randomFloat.toString)

    val cdsAccounts = Seq(
      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eori1,
            false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false)
        )
      ),

      CDSAccounts(
        eoriNumber,
        None,
        Seq(
          DutyDefermentAccount(
            dan2,
            eori2,
            false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false)
        ))
    )

    val accountLinks = Seq(
      AccountLink(),
      AccountLink())

    val model =
      FinancialsHomeModel(eoriNumber, companyName, cdsAccounts, notificationMessageKeys = List(), accountLinks, None)

  }
}

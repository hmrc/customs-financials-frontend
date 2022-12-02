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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import utils.SpecBase

// scalastyle:off magic.number
class CustomsAccountsSpec extends SpecBase {

  private val traderEori = "12345678"
  private val agentEori =  "09876543"

  val guaranteeAccount = GeneralGuaranteeAccount("G123456", traderEori, AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(1000000), BigDecimal(200000))))
  val guaranteeAccountZeroLimit = GeneralGuaranteeAccount("G123456", traderEori, AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(0), BigDecimal(200001))))
  val guaranteeAccountZeroBalance = GeneralGuaranteeAccount("G123456", traderEori, AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(200002), BigDecimal(0))))
  val guaranteeAccountZeroLimitZeroBalance = GeneralGuaranteeAccount("G123456", traderEori, AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(0), BigDecimal(0))))
  val dd1 = DutyDefermentAccount("1231231231", traderEori,AccountStatusOpen, DirectDebitMandateCancelled, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)
  val dd2 = DutyDefermentAccount("7567567567", traderEori,AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), None,None), viewBalanceIsGranted = true, isIsleOfMan = false)
  val dd3 = DutyDefermentAccount("7897897897", agentEori,AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)
  val dd4 = DutyDefermentAccount("1112223334", agentEori,AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), None,None), viewBalanceIsGranted = true, isIsleOfMan = false)

  private val cashAccountNumber = "987654"
  val cashAccount = CashAccount(cashAccountNumber, traderEori, AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(999.99))))

  val traderAccounts = List(guaranteeAccount, dd1, dd2, cashAccount)
  val traderCdsAccounts = CDSAccounts(traderEori, traderAccounts)

  val agentClientsAccounts = List(dd1, dd2, cashAccount)
  val agentOwnAccounts = List(dd3, dd4)
  val agentAccounts = agentClientsAccounts ++ agentOwnAccounts
  val agentCdsAccounts = CDSAccounts(agentEori, agentAccounts)

  "CDSAccounts" should {

    import CDSAccounts._

    "isAgent" should {
      "return false when user has no authority to view clients accounts" in {
        withClue("isAgent() must be false:") {
          traderCdsAccounts.isAgent must be (false)
        }
      }
      "return true when user has authority to view clients accounts" in {
        withClue("isAgent() must be true:") {
          agentCdsAccounts.isAgent must be (true)
        }
      }
    }

    "myAccounts" should {
      "return all accounts owned by the given EORI" in {
        agentCdsAccounts.myAccounts must be (agentOwnAccounts)
      }
    }

    "authorizedToView" should {
      "return all accounts not owned by the given EORI" in {
        agentCdsAccounts.authorizedToView must be (agentClientsAccounts)
      }
    }

    "filterCashAccounts" should {
      "return any Cash Accounts" in {
        filterCashAccounts(traderAccounts) must be (List(cashAccount))
      }
    }

    "filterDutyDefermentAccounts" should {
      "return just Duty Deferment Accounts" in {
        filterDutyDefermentAccounts(traderAccounts) must be (List(dd1, dd2))
      }
    }

    "filterGuaranteeAccounts" should {
      "return just Guarantee Accounts" in {
        filterGuaranteeAccounts(traderAccounts) must be (List(guaranteeAccount))
      }
    }

    "filterByAccountNumber" should {
      "return a function which returns any accounts which match the given account number" in {
        val func = filterByAccountNumber(cashAccountNumber)
        func(traderAccounts) must be (List(cashAccount))
      }
    }

    "DutyDefermentAccount" should {
     val values = Table(
        ("status","isleOfManFlag","expectedResult"),
        (DefermentAccountAvailable, false, false),
        (DefermentAccountAvailable, true, false),
        (ChangeOfLegalEntity, false, false),
        (ChangeOfLegalEntity, true, false),
        (GuaranteeCancelledGuarantorsRequest, false, false),
        (GuaranteeCancelledGuarantorsRequest, true, false),
        (GuaranteeCancelledTradersRequest, false, false),
        (GuaranteeCancelledTradersRequest, true, false),
        (DirectDebitMandateCancelled, false, true),
        (DirectDebitMandateCancelled, true, false),
        (DebitRejectedAccountClosedOrTransferred, false, false),
        (DebitRejectedAccountClosedOrTransferred, true, false),
        (DebitRejectedReferToDrawer, false, false),
        (DebitRejectedReferToDrawer, true, false),
        (ReturnedMailOther, false, false),
        (ReturnedMailOther, true, false),
        (GuaranteeExceeded, false, false),
        (GuaranteeExceeded, true, false),
        (AccountCancelled, false, false),
        (AccountCancelled, true, false)
      )
      "return correct requiredDirectDebit status" in {
        forAll(values) {
          (statusId: CDSAccountStatusId,isleOfManFlag:Boolean,expectedResult:Boolean) => {
            val dd =  dd1.copy(statusId = statusId, isIsleOfMan = isleOfManFlag)
            dd.isDirectDebitSetupRequired mustBe expectedResult
          }
        }
      }
    }
  }

  "GeneralGuaranteeBalance" should {

    "return correct used funds value" in {
      val expectedUsedFunds = 800000
      guaranteeAccount.balances.get.usedFunds  must be (expectedUsedFunds)
    }

    "return zero used funds when the guarantee limit is zero" in {
      val expectedUsedFunds = -200001
      guaranteeAccountZeroLimit.balances.get.usedFunds  must be (expectedUsedFunds)
    }

    "return correct used percentage value" in {
      val expectedUsedPercentage = 80
      guaranteeAccount.balances.get.usedPercentage  must be (expectedUsedPercentage)
    }

    "return used funds of 100 percent when available balance is zero" in {
      val expectedUsedPercentage = 100
      guaranteeAccountZeroBalance.balances.get.usedPercentage  must be (expectedUsedPercentage)
    }

    "return zero used percentage and funds when available balance and limit are both zero" in {
      val expectedUsedPercentage = 0
      guaranteeAccountZeroLimitZeroBalance.balances.get.usedPercentage  must be (expectedUsedPercentage)
    }

  }

  "DutyDefermentBalance" should {

    "return correct used funds value" in {
      val expectedUsedFunds = 80
      dd1.balances.usedFunds  must be (expectedUsedFunds)
    }

    "return correct used percentage value" in {
      val expectedUsedPercentage = 80
      dd1.balances.usedPercentage  must be (expectedUsedPercentage)
    }

    "usedFunds" should {
      "return zero if periodAvailableAccountBalance or periodAccountLimit is not provided" in {
        dd2.balances.usedFunds must be (0)
      }

      "return calculated funds if periodAccountLimit & periodAvailableAccountBalance are positives" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(100)), None, Some(BigDecimal(50)))
        balance.usedFunds must be (50)
      }

      "return calculated funds if periodAccountLimit is positive & periodAvailableAccountBalance is negative" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(100)), None, Some(BigDecimal(-50)))
        balance.usedFunds must be (150)
      }

      "return calculated funds if periodAccountLimit is positive & periodAvailableAccountBalance is zero" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(100)), None, Some(BigDecimal(0)))
        balance.usedFunds must be (100)
      }

      "return calculated funds if periodAccountLimit is zero & periodAvailableAccountBalance is negative" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(-100)))
        balance.usedFunds must be (100)
      }
    }

    "usedPercentage" should {
      "return zero if periodAvailableAccountBalance or periodAccountLimit is not provided" in {
        dd2.balances.usedPercentage must be (0)
      }

      "return calculated value if periodAccountLimit & periodAvailableAccountBalance are positives" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(100)), None, Some(BigDecimal(50)))
        balance.usedPercentage must be (50)
      }

      "return 100 if periodAccountLimit is positive & periodAvailableAccountBalance is zero" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(100)), None, Some(BigDecimal(0)))
        balance.usedPercentage must be (100)
      }
    }

    "availableBalance" should {
      "return zero if periodAvailableAccountBalance is not provided" in {
        dd2.balances.availableBalance must be (0)
      }

      "return the balance if periodAvailableAccountBalance is a positive value" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(50)))
        balance.availableBalance must be (50)
      }

      "return the view available balance if periodAvailableAccountBalance and periodAvailableGuaranteeBalance values are available" in {
        val balance = DutyDefermentBalance(None, None, Some(BigDecimal(100)), Some(BigDecimal(100)))
        balance.availableBalance must be (100)
      }

      "return the view available balance if periodAvailableAccountBalance is zero and periodAvailableGuaranteeBalance value is available" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(100)))
        balance.availableBalance must be (100)
      }

      "return the balance if periodAvailableAccountBalance is a zero value" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(0)))
        balance.availableBalance must be (0)
      }

      "return the balance if periodAvailableAccountBalance is a negative value" in {
        val balance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(-50)))
        balance.availableBalance must be (-50)
      }
    }

    "not throw an exception for any input values" in {
      // poor man's property-based testing...
      val sampleValues = List(Some(BigDecimal(10)), Some(BigDecimal(100)), Some(BigDecimal(-10)), Some(BigDecimal(-100)), Some(BigDecimal(0)), None)
      for {
        periodGuaranteeLimit <- sampleValues
        periodAccountLimit <- sampleValues
        periodAvailableGuaranteeBalance <- sampleValues
        periodAvailableAccountBalance <- sampleValues
      } yield {
        withClue(s"DutyDefermentBalance($periodGuaranteeLimit, $periodAccountLimit, $periodAvailableGuaranteeBalance, $periodAvailableAccountBalance)") {
          noException should be thrownBy DutyDefermentBalance(
            periodGuaranteeLimit, periodAccountLimit, periodAvailableGuaranteeBalance, periodAvailableAccountBalance
          )
        }
      }
    }
  }
}

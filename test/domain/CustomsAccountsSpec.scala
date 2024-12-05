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

import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table
import utils.SpecBase
import utils.MustMatchers
import utils.TestData.{
  BALANCE_10,
  BALANCE_100,
  BALANCE_1000000,
  BALANCE_150,
  BALANCE_20,
  BALANCE_200,
  BALANCE_200000,
  BALANCE_200001,
  BALANCE_200002,
  BALANCE_50,
  NEGATIVE_BALANCE_10,
  NEGATIVE_BALANCE_100,
  NEGATIVE_BALANCE_50
}

class CustomsAccountsSpec extends SpecBase with MustMatchers {

  "CDSAccounts" should {

    import CDSAccounts._

    "isAgent" should {
      "return false when user has no authority to view clients accounts" in new Setup {
        withClue("isAgent() must be false:") {
          traderCdsAccounts.isAgent must be(false)
        }
      }
      "return true when user has authority to view clients accounts" in new Setup {
        withClue("isAgent() must be true:") {
          agentCdsAccounts.isAgent must be(true)
        }
      }
    }

    "myAccounts" should {
      "return all accounts owned by the given EORI" in new Setup {
        agentCdsAccounts.myAccounts must be(agentOwnAccounts)
      }
    }

    "authorizedToView" should {
      "return all accounts not owned by the given EORI" in new Setup {
        agentCdsAccounts.authorizedToView must be(agentClientsAccounts)
      }
    }

    "filterCashAccounts" should {
      "return any Cash Accounts" in new Setup {
        filterCashAccounts(traderAccounts) must be(List(cashAccount))
      }
    }

    "filterDutyDefermentAccounts" should {
      "return just Duty Deferment Accounts" in new Setup {
        filterDutyDefermentAccounts(traderAccounts) must be(List(dd1, dd2))
      }
    }

    "filterGuaranteeAccounts" should {
      "return just Guarantee Accounts" in new Setup {
        filterGuaranteeAccounts(traderAccounts) must be(List(guaranteeAccount))
      }
    }

    "filterByAccountNumber" should {
      "return a function which returns any accounts which match the given account number" in new Setup {
        val func: Seq[CDSAccount] => Seq[CDSAccount] = filterByAccountNumber(cashAccountNumber)

        func(traderAccounts) must be(List(cashAccount))
      }
    }

    "DutyDefermentAccount" should {
      val values = Table(
        ("status", "isleOfManFlag", "expectedResult"),
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

      "return correct requiredDirectDebit status" in new Setup {
        forAll(values) { (statusId: CDSAccountStatusId, isleOfManFlag: Boolean, expectedResult: Boolean) =>
          {
            val dd = dd1.copy(statusId = statusId, isIsleOfMan = isleOfManFlag)
            dd.isDirectDebitSetupRequired mustBe expectedResult
          }
        }
      }
    }
  }

  "GeneralGuaranteeBalance" should {

    "return correct used funds value" in new Setup {
      val expectedUsedFunds = 800000
      guaranteeAccount.balances.get.usedFunds must be(expectedUsedFunds)
    }

    "return zero used funds when the guarantee limit is zero" in new Setup {
      val expectedUsedFunds: Int = -200001
      guaranteeAccountZeroLimit.balances.get.usedFunds must be(expectedUsedFunds)
    }

    "return correct used percentage value" in new Setup {
      val expectedUsedPercentage = 80
      guaranteeAccount.balances.get.usedPercentage must be(expectedUsedPercentage)
    }

    "return used funds of 100 percent when available balance is zero" in new Setup {
      val expectedUsedPercentage = 100
      guaranteeAccountZeroBalance.balances.get.usedPercentage must be(expectedUsedPercentage)
    }

    "return zero used percentage and funds when available balance and limit are both zero" in new Setup {
      val expectedUsedPercentage = 0
      guaranteeAccountZeroLimitZeroBalance.balances.get.usedPercentage must be(expectedUsedPercentage)
    }
  }

  "DutyDefermentBalance" should {
    "return correct used funds value" in new Setup {
      val expectedUsedFunds = 80
      dd1.balances.usedFunds must be(expectedUsedFunds)
    }

    "return correct used percentage value" in new Setup {
      val expectedUsedPercentage = 80
      dd1.balances.usedPercentage must be(expectedUsedPercentage)
    }

    "usedFunds" should {
      "return zero if periodAvailableAccountBalance or periodAccountLimit is not provided" in new Setup {
        dd2.balances.usedFunds must be(0)
      }

      "return funds if periodAccountLimit & periodAvailableAccountBalance are positives" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(BALANCE_100)), None, Some(BigDecimal(BALANCE_50)))

        balance.usedFunds must be(BALANCE_50)
      }

      "return funds if periodAccountLimit is positive & periodAvailableAccountBalance is negative" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(BALANCE_100)), None, Some(BigDecimal(NEGATIVE_BALANCE_50)))

        balance.usedFunds must be(BALANCE_150)
      }

      "return funds if periodAccountLimit is positive & periodAvailableAccountBalance is zero" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(BALANCE_100)), None, Some(BigDecimal(0)))

        balance.usedFunds must be(BALANCE_100)
      }

      "return funds if periodAccountLimit is zero & periodAvailableAccountBalance is negative" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(NEGATIVE_BALANCE_100)))

        balance.usedFunds must be(BALANCE_100)
      }
    }

    "usedPercentage" should {
      "return zero if periodAvailableAccountBalance or periodAccountLimit is not provided" in new Setup {
        dd2.balances.usedPercentage must be(0)
      }

      "return calculated value if periodAccountLimit & periodAvailableAccountBalance are positives" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(BALANCE_100)), None, Some(BigDecimal(BALANCE_50)))

        balance.usedPercentage must be(BALANCE_50)
      }

      "return 100 if periodAccountLimit is positive & periodAvailableAccountBalance is zero" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(BALANCE_100)), None, Some(BigDecimal(0)))

        balance.usedPercentage must be(BALANCE_100)
      }
    }

    "availableBalance" should {
      "return zero if periodAvailableAccountBalance is not provided" in new Setup {
        dd2.balances.availableBalance must be(0)
      }

      "return the balance if periodAvailableAccountBalance is a positive value" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(BALANCE_50)))

        balance.availableBalance must be(BALANCE_50)
      }

      "return the balance if periodAvailableAccountBalance is a zero value" in new Setup {
        val balance: DutyDefermentBalance = DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(0)))

        balance.availableBalance must be(0)
      }

      "return the balance if periodAvailableAccountBalance is a negative value" in new Setup {
        val balance: DutyDefermentBalance =
          DutyDefermentBalance(None, Some(BigDecimal(0)), None, Some(BigDecimal(NEGATIVE_BALANCE_50)))

        balance.availableBalance must be(NEGATIVE_BALANCE_50)
      }
    }

    "not throw an exception for any input values" in new Setup {
      val sampleValues: List[Option[BigDecimal]] = List(
        Some(BigDecimal(BALANCE_10)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(NEGATIVE_BALANCE_10)),
        Some(BigDecimal(NEGATIVE_BALANCE_100)),
        Some(BigDecimal(0)),
        None
      )

      for {
        periodGuaranteeLimit <- sampleValues
        periodAccountLimit <- sampleValues
        periodAvailableGuaranteeBalance <- sampleValues
        periodAvailableAccountBalance <- sampleValues
      } yield {
        withClue(
          s"DutyDefermentBalance($periodGuaranteeLimit, $periodAccountLimit," +
            s"$periodAvailableGuaranteeBalance, $periodAvailableAccountBalance)"
        ) {
          noException should be thrownBy DutyDefermentBalance(
            periodGuaranteeLimit,
            periodAccountLimit,
            periodAvailableGuaranteeBalance,
            periodAvailableAccountBalance
          )
        }
      }
    }
  }

  trait Setup {
    private val traderEori = "12345678"
    private val agentEori = "09876543"

    val guaranteeAccount: GeneralGuaranteeAccount = GeneralGuaranteeAccount(
      "G123456",
      traderEori,
      AccountStatusOpen,
      DefermentAccountAvailable,
      Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_1000000), BigDecimal(BALANCE_200000)))
    )

    val guaranteeAccountZeroLimit: GeneralGuaranteeAccount =
      GeneralGuaranteeAccount(
        "G123456",
        traderEori,
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(0), BigDecimal(BALANCE_200001)))
      )

    val guaranteeAccountZeroBalance: GeneralGuaranteeAccount =
      GeneralGuaranteeAccount(
        "G123456",
        traderEori,
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_200002), BigDecimal(0)))
      )

    val guaranteeAccountZeroLimitZeroBalance: GeneralGuaranteeAccount =
      GeneralGuaranteeAccount(
        "G123456",
        traderEori,
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(0), BigDecimal(0)))
      )

    val dd1: DutyDefermentAccount = DutyDefermentAccount(
      "1231231231",
      traderEori,
      isNiAccount = false,
      AccountStatusOpen,
      DirectDebitMandateCancelled,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_200)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(BALANCE_50)),
        Some(BigDecimal(BALANCE_20))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd2: DutyDefermentAccount = DutyDefermentAccount(
      "7567567567",
      traderEori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(BALANCE_200)), Some(BigDecimal(BALANCE_100)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd3: DutyDefermentAccount = DutyDefermentAccount(
      "7897897897",
      agentEori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_200)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(BALANCE_50)),
        Some(BigDecimal(BALANCE_20))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd4: DutyDefermentAccount = DutyDefermentAccount(
      "1112223334",
      agentEori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(BALANCE_200)), Some(BigDecimal(BALANCE_100)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val cashAccountNumber = "987654"

    val cashAccount: CashAccount = CashAccount(
      cashAccountNumber,
      traderEori,
      AccountStatusOpen,
      DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(999.99)))
    )

    val traderAccounts: List[CDSAccount] = List(guaranteeAccount, dd1, dd2, cashAccount)
    val traderCdsAccounts: CDSAccounts = CDSAccounts(traderEori, None, traderAccounts)
    val agentClientsAccounts: List[CDSAccount] = List(dd1, dd2, cashAccount)
    val agentOwnAccounts: List[DutyDefermentAccount] = List(dd3, dd4)
    val agentAccounts: List[CDSAccount] = agentClientsAccounts ++ agentOwnAccounts
    val agentCdsAccounts: CDSAccounts = CDSAccounts(agentEori, None, agentAccounts)
  }
}

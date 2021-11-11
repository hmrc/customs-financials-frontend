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

package viewmodels

import domain.{AccountCancelled, AccountStatusClosed, AccountStatusOpen, AccountStatusSuspended, DefermentAccountAvailable, DirectDebitMandateCancelled, GeneralGuaranteeAccount, GeneralGuaranteeBalance}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers._
import utils.SpecBase

class GeneralGuaranteeAccountViewModelSpec extends SpecBase    {

  "ViewModel" when {

    "account status is open" should {
      val account = GeneralGuaranteeAccount("GAN1234", "EORI1234", AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(100, 100))) //scalastyle:off magic.number
      val viewModel = GeneralGuaranteeAccountViewModel(account)

      "have 'account-status-open' status html class attribute" in {
        viewModel.statusHtmlClassAttribute mustBe "govuk-tag"
      }

      "have 'account-balance-status-open' balance html class attribute" in {
        viewModel.balanceHtmlClassAttribute mustBe "account-balance-status-open"
      }

    }

    "account status with no balances is open" should {
      val account = GeneralGuaranteeAccount("GAN1234", "EORI1234", AccountStatusOpen, DefermentAccountAvailable, None) //scalastyle:off magic.number
      val viewModel = GeneralGuaranteeAccountViewModel(account)

      "have 'account-status-open' status html class attribute" in {
        viewModel.statusHtmlClassAttribute mustBe "govuk-tag"
      }

      "have 'account-balance-status-open' balance html class attribute" in {
        viewModel.balanceHtmlClassAttribute mustBe "account-balance-status-open"
      }

    }

    "account status is suspended" should {
      val account = GeneralGuaranteeAccount("GAN1234", "EORI1234", AccountStatusSuspended, DirectDebitMandateCancelled, Some(GeneralGuaranteeBalance(100, 100))) //scalastyle:off magic.number
      val viewModel = GeneralGuaranteeAccountViewModel(account)

      "have 'account-status-suspended' status html class attribute" in {
        viewModel.statusHtmlClassAttribute mustBe "govuk-tag govuk-tag--yellow"
      }

      "have 'account-balance-status-suspended' balance html class attribute" in {
        viewModel.balanceHtmlClassAttribute mustBe "account-balance-status-suspended"
      }

    }

    "account status is closed" should {
      val account = GeneralGuaranteeAccount("GAN1234", "EORI1234", AccountStatusClosed, AccountCancelled, Some(GeneralGuaranteeBalance(100, 100))) //scalastyle:off magic.number
      val viewModel = GeneralGuaranteeAccountViewModel(account)

      "have 'account-status-closed' status html class attribute" in {
        viewModel.statusHtmlClassAttribute mustBe "govuk-tag govuk-tag--grey"
      }

      "have 'account-balance-status-closed' balance html class attribute" in {
        viewModel.balanceHtmlClassAttribute mustBe "account-balance-status-closed"
      }

    }

  }
}

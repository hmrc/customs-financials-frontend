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

class AccountsAndBalancesResponseContainerSpec extends SpecBase {

  "toCdsAccounts" should {

    "return empty accounts" when {
      "there is no DD, GA and Cach accounts in the response" in {

        val eori = "GB12345678"

        val accResponseWithNoAccounts: AccountsAndBalancesResponseContainer = AccountsAndBalancesResponseContainer(
          AccountsAndBalancesResponse(
            None,
            domain.AccountResponseDetail(
              Some(eori),
              None,
              None,
              None,
              None))
        )

        accResponseWithNoAccounts.toCdsAccounts(eori) mustBe CDSAccounts(eori, isNiAccount = Some(false), Seq.empty)
      }
    }
  }
}
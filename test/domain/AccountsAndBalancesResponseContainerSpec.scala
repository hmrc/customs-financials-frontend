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
import utils.MustMatchers
import play.api.libs.json.{JsResultException, JsSuccess, Json}
import utils.TestData.TEST_EORI

class AccountsAndBalancesResponseContainerSpec extends SpecBase with MustMatchers {

  "toCdsAccounts" should {

    "return empty accounts" when {
      "there is no DD, GA and Cach accounts in the response" in {

        val accResponseWithNoAccounts: AccountsAndBalancesResponseContainer = AccountsAndBalancesResponseContainer(
          AccountsAndBalancesResponse(None, domain.AccountResponseDetail(Some(TEST_EORI), None, None, None, None))
        )

        accResponseWithNoAccounts
          .toCdsAccounts(TEST_EORI) mustBe CDSAccounts(TEST_EORI, isNiAccount = Some(false), Seq.empty)
      }
    }
  }

  "Json Reads" should {
    "generate the correct object from valid Json" in {
      import AccountsAndBalancesResponseContainer.reads

      val accResponseWithNoAccounts: AccountsAndBalancesResponseContainer = AccountsAndBalancesResponseContainer(
        AccountsAndBalancesResponse(None, domain.AccountResponseDetail(Some(TEST_EORI), None, None, None, None))
      )

      val accResJsString =
        """{"accountsAndBalancesResponse":{"responseDetail":{"EORINo":"GB12345678"}}}""".stripMargin

      Json.fromJson(Json.parse(accResJsString)) mustBe JsSuccess(accResponseWithNoAccounts)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"accountsAndBalancesResponse1\": \"pending\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AccountsAndBalancesResponseContainer]
      }
    }
  }
}

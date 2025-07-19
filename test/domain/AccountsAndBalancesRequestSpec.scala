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

import utils.{MustMatchers, SpecBase}
import play.api.libs.json.{JsResultException, JsSuccess, Json}
import utils.TestData.{ACC_NUMBER, ACC_TYPE_CDS_CASH, REGIME_CDS, TEST_ACK_REF, TEST_DATE, TEST_EORI}

class AccountsAndBalancesRequestSpec extends SpecBase with MustMatchers {

  "format" should {

    "generate correct output for Json Reads" in new Setup {
      import AccountsAndBalancesRequest.format

      Json.fromJson(Json.parse(accAndBalJsString)) mustBe JsSuccess(accAndBalanceReqOb)
    }

    "throw exception for Invalid JSON" in {
      val invalidJson = "{ \"status\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AccountsAndBalancesRequest]
      }
    }
  }

  trait Setup {
    val requestCommonOb: AccountsRequestCommon =
      AccountsRequestCommon(receiptDate = TEST_DATE, acknowledgementReference = TEST_ACK_REF, regime = REGIME_CDS)

    val requestDetailOb: AccountsRequestDetail = AccountsRequestDetail(
      EORINo = TEST_EORI,
      accountType = Some(ACC_TYPE_CDS_CASH),
      accountNumber = Some(ACC_NUMBER),
      referenceDate = Some(TEST_DATE)
    )

    val accAndBalanceReqOb: AccountsAndBalancesRequest = AccountsAndBalancesRequest(requestCommonOb, requestDetailOb)

    val accAndBalJsString: String =
      """{"requestCommon":{"receiptDate":"2024-10-01","acknowledgementReference":"test_ref","regime":"CDS"},
        |"requestDetail":{"EORINo":"GB12345678","accountType":"Cash account",
        |"accountNumber":"1234567","referenceDate":"2024-10-01"}}""".stripMargin
  }
}

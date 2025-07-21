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
import utils.TestData.{ACC_NUMBER, TEST_DATE, TEST_STATUS, TEST_STATUS_TEXT}
import domain.DutyPaymentMethod.CDS

class AccountsAndBalancesResponseSpec extends SpecBase with MustMatchers {

  "Json Reads" should {

    "generate correct output for valid Json" in {
      import AccountsAndBalancesResponse.reads

      val resCommonOb = AccountResponseCommon(
        status = TEST_STATUS,
        statusText = Some(TEST_STATUS_TEXT),
        processingDate = TEST_DATE,
        returnParameters = None
      )

      val eori          = "test_eori"
      val referenceDate = "2023-10-12"

      val accRes: AccountResponse = AccountResponse(ACC_NUMBER, "dd", CDS, None, None, viewBalanceIsGranted = true)

      val ddAccRes: DutyDefermentAccountResponse =
        DutyDefermentAccountResponse(account = accRes, limits = None, balances = None)

      val ggAccRes: GeneralGuaranteeAccountResponse = GeneralGuaranteeAccountResponse(accRes, None, None)

      val cashAccRes: CdsCashAccountResponse = CdsCashAccountResponse(account = accRes, availableAccountBalance = None)

      val accResDetailObWithAccount: AccountResponseDetail =
        AccountResponseDetail(
          Some(eori),
          Some(referenceDate),
          Some(Seq(ddAccRes)),
          Some(Seq(ggAccRes)),
          Some(Seq(cashAccRes))
        )

      val accAndBalResOb = AccountsAndBalancesResponse(Some(resCommonOb), accResDetailObWithAccount)

      val accAndBalResJsString =
        """{"responseCommon":{"status":"pending","statusText":"test_status","processingDate":"2024-10-01"},
          |"responseDetail":{"EORINo":"test_eori","referenceDate":"2023-10-12","dutyDefermentAccount":[
          |{"account":{"number":"1234567","type":"dd","owner":"CDS","viewBalanceIsGranted":true},
          |"isIomAccount":false,"isNiAccount":false}],
          |"generalGuaranteeAccount":[
          |{"account":{"number":"1234567","type":"dd","owner":"CDS","viewBalanceIsGranted":true}}],
          |"cdsCashAccount":[
          |{"account":{"number":"1234567","type":"dd","owner":"CDS","viewBalanceIsGranted":true}}]}}""".stripMargin

      Json.fromJson(Json.parse(accAndBalResJsString)) mustBe JsSuccess(accAndBalResOb)
    }

    "throw exception for Invalid JSON" in {
      val invalidJson = "{ \"status\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AccountsAndBalancesResponse]
      }
    }
  }
}

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
import play.api.libs.json.{JsSuccess, Json}
import AccountResponse.reads

class AccountResponseSpec extends SpecBase with MustMatchers {

  "reads" should {

    "create the object correctly" when {

      "isleOfManFlag is absence in the json representation" in new Setup {
        Json.fromJson(Json.parse(accResResponseString)) mustBe JsSuccess(accResWithDefaultIOMFlag)
      }

      "isleOfManFlag value is present in the json representation" in new Setup {
        Json.fromJson(Json.parse(accResResponseWithIOMValueString)) mustBe JsSuccess(accResWithIOMFlag)
      }
    }
  }

  trait Setup {

    val accResWithDefaultIOMFlag: AccountResponse = AccountResponse(
      number = "12345678",
      `type` = "DutyDeferment",
      owner = "test_eori",
      accountStatus = Some(AccountStatusSuspended),
      accountStatusID = Some(DirectDebitMandateCancelled),
      viewBalanceIsGranted = true
    )

    val accResWithIOMFlag: AccountResponse = AccountResponse(
      number = "12345678",
      `type` = "DutyDeferment",
      owner = "test_eori",
      accountStatus = Some(AccountStatusSuspended),
      accountStatusID = Some(DirectDebitMandateCancelled),
      viewBalanceIsGranted = true,
      isleOfManFlag = Some(true)
    )

    val accResResponseWithIOMValueString: String =
      """{
        |"number":"12345678",
        |"accountStatusID":4,
        |"accountStatus":"suspended",
        |"owner":"test_eori",
        |"type":"DutyDeferment",
        |"viewBalanceIsGranted":true,
        |"isleOfManFlag":true
        |}""".stripMargin

    val accResResponseString: String =
      """{
        |"number":"12345678",
        |"accountStatusID":4,
        |"accountStatus":"suspended",
        |"owner":"test_eori",
        |"type":"DutyDeferment",
        |"viewBalanceIsGranted":true
        |}""".stripMargin
  }
}

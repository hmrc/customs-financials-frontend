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

class AuthorisedAccountsSpec extends SpecBase with MustMatchers {

  "AuthorisedGeneralGuaranteeAccount.format" should {

    "generate correct output for Json Reads" in new Setup {
      import AuthorisedGeneralGuaranteeAccount.format

      Json.fromJson(Json.parse(authGGAccJsString)) mustBe JsSuccess(authGGAccOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(authGGAccOb) mustBe Json.parse(authGGAccJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"account1\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AuthorisedGeneralGuaranteeAccount]
      }
    }
  }

  "AuthorisedDutyDefermentAccount.format" should {

    "generate correct output for Json Reads" in new Setup {

      import AuthorisedDutyDefermentAccount.format

      Json.fromJson(Json.parse(authDDAccJsString)) mustBe JsSuccess(authDDAccOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(authDDAccOb) mustBe Json.parse(authDDAccJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"account1\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AuthorisedDutyDefermentAccount]
      }
    }
  }

  "AuthorisedCashAccount.format" should {

    "generate correct output for Json Reads" in new Setup {

      import AuthorisedCashAccount.format

      Json.fromJson(Json.parse(authCashAccJsString)) mustBe JsSuccess(authCashAccOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(authCashAccOb) mustBe Json.parse(authCashAccJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"account1\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AuthorisedCashAccount]
      }
    }
  }

  trait Setup {
    val authGGAccOb: AuthorisedGeneralGuaranteeAccount =
      AuthorisedGeneralGuaranteeAccount(
        account = Account("1234", "GeneralGuarantee", "GB000000000000"),
        availableGuaranteeBalance = Some("100")
      )

    val authGGAccJsString: String =
      """{"account":{"accountNumber":"1234","accountType":"GeneralGuarantee","accountOwner":"GB000000000000"},
        |"availableGuaranteeBalance":"100"}""".stripMargin

    val authDDAccOb: AuthorisedDutyDefermentAccount =
      AuthorisedDutyDefermentAccount(
        Account("1234", "GeneralGuarantee", "GB000000000000"),
        None
      )

    val authDDAccJsString: String =
      """{"account":{
        |"accountNumber":"1234","accountType":"GeneralGuarantee","accountOwner":"GB000000000000"}
        |}""".stripMargin

    val authCashAccOb: AuthorisedCashAccount =
      AuthorisedCashAccount(account = Account("1234", "CDSCashAccount", "GB000000000000"), None)

    val authCashAccJsString: String =
      """{"account":{
        |"accountNumber":"1234","accountType":"CDSCashAccount","accountOwner":"GB000000000000"}
        |}""".stripMargin
  }
}

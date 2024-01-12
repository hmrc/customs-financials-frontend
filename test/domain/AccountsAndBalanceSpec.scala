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

import domain.DutyPaymentMethod.CDS
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.Json
import utils.SpecBase

class AccountsAndBalanceSpec extends SpecBase {

  "AccountsAndBalances" should {
    "be able parse account status from json" in {

      val statusAndCode = List(
        "Open" -> AccountStatusOpen,
        "open" -> AccountStatusOpen,
        "Suspended" -> AccountStatusSuspended,
        "suspended" -> AccountStatusSuspended,
        "Closed" -> AccountStatusClosed,
        "closed" -> AccountStatusClosed,
        "Unknown" -> AccountStatusOpen
      )

      statusAndCode.foreach { case (statusCode, status) =>
        val json =
          s"""{
             |  "number": "123456",
             |  "type": "GeneralGuarantee",
             |  "owner": "EORI1234",
             |  "accountStatus": "$statusCode",
             |  "viewBalanceIsGranted": true
             |}""".stripMargin

        val account = Json.parse(json).as[AccountResponse]

        val expectedAccount = AccountResponse("123456",
          "GeneralGuarantee", "EORI1234", Some(status), viewBalanceIsGranted = true,
          accountStatusID = None, isleOfManFlag = None)

        account mustBe expectedAccount
      }
    }

    "be parse account when status is none from json" in {
      val json =
        s"""{
           |  "number": "123456",
           |  "type": "DutyDeferment",
           |  "owner": "EORI1234",
           |  "viewBalanceIsGranted": true
           |}""".stripMargin

      val account = Json.parse(json).as[AccountResponse]

      val expectedAccount = AccountResponse("123456", "DutyDeferment", "EORI1234",
        None, viewBalanceIsGranted = true, accountStatusID = None, isleOfManFlag = None)

      account mustBe expectedAccount
    }

    "be able parse account when isleOfManFlag is set for json" in {
      val json =
        s"""{
           |  "number": "123456",
           |  "type": "DutyDeferment",
           |  "owner": "EORI1234",
           |  "viewBalanceIsGranted": true,
           |  "isleOfManFlag": true
           |}""".stripMargin

      val account = Json.parse(json).as[AccountResponse]

      val expectedAccount = AccountResponse("123456", "DutyDeferment", "EORI1234", None,
        viewBalanceIsGranted = true, accountStatusID = None, isleOfManFlag = Some(true))

      account mustBe expectedAccount
    }

    "be able to generate common request without PID and originatingSystem when 'useACC27' is true" in {
      val requestDud09 = AccountsRequestCommon.generate
      requestDud09.receiptDate.isEmpty mustBe false
      requestDud09.acknowledgementReference.size mustBe 32
      requestDud09.regime mustBe CDS
    }

    "be able to serialise and deserialise CDSAccountStatusIDs" in {

      val initialAccountStatusID: CDSAccountStatusId = DefermentAccountAvailable
      val js = Json.toJson(initialAccountStatusID)
      val fromJson = Json.fromJson[CDSAccountStatusId](js).get

      fromJson mustBe initialAccountStatusID

    }
  }
}

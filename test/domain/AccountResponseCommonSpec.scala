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
import utils.TestData.{TEST_STATUS, TEST_STATUS_TEXT}

class AccountResponseCommonSpec extends SpecBase with MustMatchers {

  "Json Reads" should {

    "generate correct output" in {
      import AccountResponseCommon.reads

      val accResCommonJsString    =
        """{"status":"pending","statusText":"test_status","processingDate":"test_data"}""".stripMargin
      val accountResponseCommonOb = AccountResponseCommon(
        status = TEST_STATUS,
        statusText = Some(TEST_STATUS_TEXT),
        processingDate = "test_data",
        returnParameters = None
      )

      Json.fromJson(Json.parse(accResCommonJsString)) mustBe JsSuccess(accountResponseCommonOb)
    }
  }

  "Invalid JSON" should {
    "fail" in {
      val invalidJson = "{ \"status\": \"pending\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AccountResponseCommon]
      }
    }
  }
}

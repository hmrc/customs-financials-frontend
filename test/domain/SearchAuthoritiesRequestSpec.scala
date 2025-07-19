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
import utils.TestData.{TEST_EORI, TEST_ID}
import play.api.libs.json.{JsResultException, JsSuccess, Json}

class SearchAuthoritiesRequestSpec extends SpecBase with MustMatchers {

  "format" should {

    "generate correct output for Json Reads" in new Setup {
      import SearchAuthoritiesRequest.format

      Json.fromJson(Json.parse(searchReqAuthoritiesReqJsString)) mustBe JsSuccess(searchReqAuthoritiesReqOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(searchReqAuthoritiesReqOb) mustBe Json.parse(searchReqAuthoritiesReqJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"account1\": \"pending\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[SearchAuthoritiesRequest]
      }
    }
  }

  trait Setup {
    val searchReqAuthoritiesReqOb: SearchAuthoritiesRequest =
      SearchAuthoritiesRequest(searchID = TEST_ID, requestingEORI = TEST_EORI)

    val searchReqAuthoritiesReqJsString: String =
      """{"searchID":"test_id","requestingEORI":"GB12345678"}""".stripMargin
  }
}

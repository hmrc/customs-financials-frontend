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

class DefermentBalancesResponseSpec extends SpecBase with MustMatchers {

  "Json Reads" should {

    "generate correct output for Json Reads" in {
      import DefermentBalancesResponse.reads

      val ddBalResOb       = DefermentBalancesResponse("100", "100")
      val ddBalResJsString =
        """{"periodAvailableGuaranteeBalance":"100", "periodAvailableAccountBalance":"100"}""".stripMargin

      Json.fromJson(Json.parse(ddBalResJsString)) mustBe JsSuccess(ddBalResOb)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"account1\": \"pending\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[DefermentBalancesResponse]
      }
    }
  }
}

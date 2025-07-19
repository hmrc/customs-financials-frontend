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

class LimitsSpec extends SpecBase with MustMatchers {

  "reads" should {
    "generate correct output for valid Json" in new Setup {
      import Limits.reads

      Json.fromJson(Json.parse(limitsJsString)) mustBe JsSuccess(limitsOb)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"periodGuaranteeLimit\": \"GB12345678\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[Limits]
      }
    }
  }

  trait Setup {
    val limitsOb: Limits       = Limits("200", "100")
    val limitsJsString: String = """{"periodGuaranteeLimit":"200","periodAccountLimit":"100"}""".stripMargin
  }
}

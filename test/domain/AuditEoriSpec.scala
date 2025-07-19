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
import utils.TestData.TEST_EORI

class AuditEoriSpec extends SpecBase with MustMatchers {

  "format" should {
    "generate correct output for Json Reads" in new Setup {

      import AuditEori.format

      Json.fromJson(Json.parse(auditEoriJsString)) mustBe JsSuccess(auditEoriOb)
    }

    "generate correct output for Json Writes" in new Setup {
      Json.toJson(auditEoriOb) mustBe Json.parse(auditEoriJsString)
    }

    "throw exception for invalid Json" in {
      val invalidJson = "{ \"eori\": \"GB12345678\", \"eventId1\": \"test_event\" }"

      intercept[JsResultException] {
        Json.parse(invalidJson).as[AuditEori]
      }
    }
  }

  trait Setup {
    val auditEoriOb: AuditEori    = AuditEori(TEST_EORI, true)
    val auditEoriJsString: String = """{"eori":"GB12345678","isHistoric":true}""".stripMargin
  }
}

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
import utils.TestData.{TEST_KEY, TEST_KEY_VALUE}

class MetadataItemSpec extends SpecBase with MustMatchers {
  "metadataItemReads" should {

    "Read the object correctly" in new Setup {
      import MetadataItem.metadataItemReads

      Json.fromJson(Json.parse(metaDataItemInputJsString)) mustBe JsSuccess(metaDataItemObject)
    }
  }

  "metadataItemWrites" should {
    "Write the object correctly" in new Setup {
      Json.toJson(metaDataItemObject) mustBe Json.parse(metaDataItemJsString)
    }
  }

  "throw exception for invalid Json" in {
    val invalidJson = "{ \"key1\": \"pending\" }"

    intercept[JsResultException] {
      Json.parse(invalidJson).as[MetadataItem]
    }
  }

  trait Setup {
    val metaDataItemObject: MetadataItem = MetadataItem(TEST_KEY, TEST_KEY_VALUE)

    val metaDataItemJsString: String      = """{"key":"test_key","value":"test_value"}""".stripMargin
    val metaDataItemInputJsString: String = """{"metadata":"test_key","value":"test_value"}""".stripMargin
  }
}

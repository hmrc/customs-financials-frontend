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

import domain.FileRole.C79Certificate
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import services.DocumentAttributes
import utils.SpecBase

class DocumentAttributesSpec extends SpecBase {

  "isRequested" should {
    val documentAttributes = DocumentAttributes(
      eori = "GB11111",
      fileRole = C79Certificate,
      fileName = "Foo.pdf",
      fileSize = 0,
      metadata = Map.empty)

    "return true" when {
      "there is a statement request id" in {
        val documentWithStatementRequestId = documentAttributes.copy(
          metadata = Map("statementRequestID" -> "12345-98765-333"))

        documentWithStatementRequestId.isRequested must be(true)
      }
    }

    "return false" when {
      "there is no statement request id" in {
        documentAttributes.isRequested must be(false)
      }
    }
  }
}

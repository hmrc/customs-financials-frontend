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

package utils

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class UtilsSpec extends SpecBase {
"isSearchQueryAnAccountNumber" should {
  "return true when input string is Account number" in {
    Utils.isSearchQueryAnAccountNumber("1234567") mustBe true
    Utils.isSearchQueryAnAccountNumber("123456789") mustBe true
    Utils.isSearchQueryAnAccountNumber("Ab34567890") mustBe true
    Utils.isSearchQueryAnAccountNumber("Ab345678") mustBe true
  }

  "return false when input string is an EORI" in {
    Utils.isSearchQueryAnAccountNumber("GB1234567789") mustBe false
    Utils.isSearchQueryAnAccountNumber("GBN1234567234") mustBe false
    Utils.isSearchQueryAnAccountNumber("XI12345670890") mustBe false
  }
}

  "emptyString" should {
    "return correct value" in {
      Utils.emptyString mustBe empty
    }
  }
}

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

package forms.mappings

import play.api.data.validation.{Invalid, Valid}
import utils.{ShouldMatchers, SpecBase}

class ConstraintsSpec extends SpecBase with Constraints with ShouldMatchers {

  "checkEORI" should {

    "return valid when GBN EORI is provided" in {
      val result = checkEORI("error.invalid2")("GBN45365789211")
      result shouldEqual Valid
    }

    "return valid when XI EORI is provided" in {
      val result = checkEORI("error.invalid2")("XI123456789102")
      result shouldEqual Valid
    }

    "return Invalid when an incorrect EORI Length is provided" in {
      val result = checkEORI("error.invalid2")("XI453")
      result shouldEqual Invalid("error.invalid2", """GB\d{12}""")
    }

    "return Valid for an input that does not match the expression" in {
      val result = checkEORI("error.invalid2")("GB123456789102")
      result shouldEqual Valid
    }

    "return valid when DAN regex is provided" in {
      val result = checkEORI("error.invalid2")("4536578")
      result shouldEqual Valid
    }

    "return valid when CAN regex is provided" in {
      val result = checkEORI("error.invalid2")("45365789211")
      result shouldEqual Valid
    }

    "return valid when GAN regex is provided" in {
      val result = checkEORI("error.invalid2")("GB365789")
      result shouldEqual Valid
    }
  }
}

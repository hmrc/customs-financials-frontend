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

package forms

import play.api.data.{Form, FormError}
import utils.{ShouldMatchers, SpecBase}

class EoriNumberFormProviderSpec extends SpecBase with ShouldMatchers {

  "apply" should {

    "create the form correctly" when {

      "Eori is an valid EU Eori" in {
        val euEori = "FR744638982004"

        val validForm: Form[String] =
          new EoriNumberFormProvider().apply().bind(Map("value" -> euEori))

        validForm.get shouldEqual euEori
      }

      "Eori is an valid GB Eori" in {
        val gbEori = "GB744638982004"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gbEori))

        validForm.get shouldEqual gbEori
      }

      "Eori is an valid XI Eori" in {
        val xiEori = "XI123456789102"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> xiEori))

        validForm.get shouldEqual xiEori
      }

      "Eori starts with GBN" in {
        val gbnEori = "GBN45365789211"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gbnEori))

        validForm.get shouldEqual gbnEori
      }

      "Input is a DAN" in {
        val dan = "4536578"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> dan))

        validForm.get shouldEqual dan
      }

      "Input is a CAN" in {
        val can = "45365789211"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> can))

        validForm.get shouldEqual can
      }

      "Input is a GAN" in {
        val gan = "GB365789"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gan))

        validForm.get shouldEqual gan
      }
    }

    "generate the form error" when {

      "Eori is blank" in {
        val form: Form[String] =
          new EoriNumberFormProvider().apply().bind(Map("value" -> emptyString))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error"), Seq.empty)
        )
      }

      "Eori is an invalid EU Eori" in {
        val invalidEUEori = "FR74463898200412345677"

        val form: Form[String] =
          new EoriNumberFormProvider().apply().bind(Map("value" -> invalidEUEori))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("^[A-Z]{2}[0-9A-Z]{1,15}$"))
        )
      }

    }
  }
}

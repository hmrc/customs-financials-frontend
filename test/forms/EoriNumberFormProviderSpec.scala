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

      "eu-eori-enabled feature flag is true and eori is an valid EU Eori" in {
        val euEori = "FR744638982004"

        val validForm: Form[String] =
          new EoriNumberFormProvider().apply(isEUEoriEnabled = true).bind(Map("value" -> euEori))

        validForm.get shouldEqual euEori
      }

      "eu-eori-enabled feature flag is false and eori is an valid GB Eori" in {
        val gbEori = "GB744638982004"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gbEori))

        validForm.get shouldEqual gbEori
      }

      "eu-eori-enabled feature flag is false and eori is an valid XI Eori" in {
        val xiEori = "XI123456789102"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> xiEori))

        validForm.get shouldEqual xiEori
      }

      "eu-eori-enabled feature flag is false and eori starts with GBN" in {
        val gbnEori = "GBN45365789211"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gbnEori))

        validForm.get shouldEqual gbnEori
      }

      "eu-eori-enabled feature flag is false and input is a DAN" in {
        val dan = "4536578"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> dan))

        validForm.get shouldEqual dan
      }

      "eu-eori-enabled feature flag is false and input is a CAN" in {
        val can = "45365789211"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> can))

        validForm.get shouldEqual can
      }

      "eu-eori-enabled feature flag is false and input is a GAN" in {
        val gan = "GB365789"

        val validForm: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> gan))

        validForm.get shouldEqual gan
      }
    }

    "generate the form error" when {

      "eu-eori-enabled feature flag is true and eori is blank" in {
        val form: Form[String] =
          new EoriNumberFormProvider().apply(isEUEoriEnabled = true).bind(Map("value" -> emptyString))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error"), Seq.empty)
        )
      }

      "eu-eori-enabled feature flag is true and eori is an invalid EU Eori" in {
        val invalidEUEori = "FR74463898200412345677"

        val form: Form[String] =
          new EoriNumberFormProvider().apply(isEUEoriEnabled = true).bind(Map("value" -> invalidEUEori))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("^[A-Z]{2}[0-9A-Z]{1,15}$"))
        )
      }

      "eu-eori-enabled feature flag is false and eori is an invalid GB Eori" in {
        val invalidGBEori = "G744638982004"

        val form: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> invalidGBEori))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("GB\\d{12}"))
        )
      }

      "eu-eori-enabled feature flag is false and eori is an invalid GBN Eori" in {
        val invalidGBNEori = "GBT744638982004"

        val form: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> invalidGBNEori))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("GB\\d{12}"))
        )
      }

      "eu-eori-enabled feature flag is false and eori is an invalid XI Eori" in {
        val invalidXIEori = "X744638982004"

        val form: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> invalidXIEori))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("GB\\d{12}"))
        )
      }

      "eu-eori-enabled feature flag is false and input is an invalid account number" in {
        val invalidAccNo = "45365789211567777"

        val form: Form[String] = new EoriNumberFormProvider().apply().bind(Map("value" -> invalidAccNo))

        form.errors shouldEqual List(
          FormError("value", List("cf.search.authorities.error.invalid"), Seq("GB\\d{12}"))
        )
      }
    }
  }
}

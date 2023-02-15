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

package mappings

import forms.mappings.Mappings
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.{Form, FormError}
import utils.SpecBase


object MappingsSpec {

  sealed trait Foo

  case object Bar extends Foo

  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)
  }

  class MappingsSpec extends SpecBase with Mappings {

    import MappingsSpec._

    "text" should {

      val testForm: Form[String] =
        Form(
          "value" -> text()
        )

      "bind a valid string" in {
        val result = testForm.bind(Map("value" -> "foobar"))
        result.get mustEqual "foobar"
      }

      "not bind an empty string" in {
        val result = testForm.bind(Map("value" -> ""))
        result.errors must contain(FormError("value", "error.required"))
      }

      "not bind an empty map" in {
        val result = testForm.bind(Map.empty[String, String])
        result.errors must contain(FormError("value", "error.required"))
      }

      "return a custom error message" in {
        val form = Form("value" -> text("custom.error"))
        val result = form.bind(Map("value" -> ""))
        result.errors must contain(FormError("value", "custom.error"))
      }

      "unbind a valid value" in {
        val result = testForm.fill("foobar")
        result.apply("value").value.value mustEqual "foobar"
      }
    }

    "boolean" should {

      val testForm: Form[Boolean] =
        Form(
          "value" -> boolean()
        )

      "bind true" in {
        val result = testForm.bind(Map("value" -> "true"))
        result.get mustEqual true
      }

      "bind false" in {
        val result = testForm.bind(Map("value" -> "false"))
        result.get mustEqual false
      }

      "not bind a non-boolean" in {
        val result = testForm.bind(Map("value" -> "not a boolean"))
        result.errors must contain(FormError("value", "error.boolean"))
      }

      "not bind an empty value" in {
        val result = testForm.bind(Map("value" -> ""))
        result.errors must contain(FormError("value", "error.required"))
      }

      "not bind an empty map" in {
        val result = testForm.bind(Map.empty[String, String])
        result.errors must contain(FormError("value", "error.required"))
      }

      "unbind" in {
        val result = testForm.fill(true)
        result.apply("value").value.value mustEqual "true"
      }
    }

    "int" should {

      val testForm: Form[Int] =
        Form(
          "value" -> int()
        )

      "bind a valid integer" in {
        val result = testForm.bind(Map("value" -> "1"))
        result.get mustEqual 1
      }

      "not bind an empty value" in {
        val result = testForm.bind(Map("value" -> ""))
        result.errors must contain(FormError("value", "error.required"))
      }

      "not bind an empty map" in {
        val result = testForm.bind(Map.empty[String, String])
        result.errors must contain(FormError("value", "error.required"))
      }

      "unbind a valid value" in {
        val result = testForm.fill(123)
        result.apply("value").value.value mustEqual "123"
      }
    }

    "decimal" should {

      val testForm: Form[String] =
        Form(
          "value" -> decimal()
        )

      "bind a valid integer" in {
        val result = testForm.bind(Map("value" -> "1.0"))
        result.get mustEqual "1.0"
      }

      "bind a valid decimal" in {
        val result = testForm.bind(Map("value" -> "1.1"))
        result.get mustEqual "1.1"
      }

      "bind a number with commas" in {
        val result = testForm.bind(Map("value" -> "1,000.10"))
        result.get mustEqual "1000.10"
      }

      "not bind an empty value" in {
        val result = testForm.bind(Map("value" -> ""))
        result.errors must contain(FormError("value", "error.required"))
      }

      "not bind an empty map" in {
        val result = testForm.bind(Map.empty[String, String])
        result.errors must contain(FormError("value", "error.required"))
      }

      "not bind a non-numeric string" in {
        val result = testForm.bind(Map("value" -> "foo"))
        result.errors must contain(FormError("value", "error.nonNumeric"))
      }

      "unbind a valid value" in {
        val result = testForm.fill("1337.1337")
        result("value").value.value mustEqual "1337.1337"
      }
    }
  }
}


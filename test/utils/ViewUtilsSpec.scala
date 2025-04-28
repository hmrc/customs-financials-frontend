/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.ViewUtils._

class ViewUtilsSpec extends SpecBase with MustMatchers with Setup {

  "ViewUtils" should {

    "return title with error prefix if form has errors" in {
      val formWithError = testForm.withError("value", "error.required")
      val result = title(formWithError, "test.title", Some("test.section"), Seq())

      result must include(messages("site.error"))
      result must include(messages("test.title"))
      result must include(messages("test.section"))
    }

    "return title without error prefix if form has no errors" in {
      val result = title(testForm, "test.title", Some("test.section"), Seq())

      result must not include messages("site.error")
      result must include(messages("test.title"))
      result must include(messages("test.section"))
    }

    "return title without section if section is None" in {
      val result = title(testForm, "test.title", None, Seq())

      result must include(messages("test.title"))
      result must not include messages("test.section")
    }
  }
}

trait Setup {
  implicit val messages: Messages = Helpers.stubMessages()

  val testForm: Form[String] = Form("value" -> nonEmptyText)
}

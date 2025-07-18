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

import play.api.data.Form
import play.api.i18n.Messages
import utils.Utils.emptyString

object ViewUtils {

  def title(form: Form[_], titleStr: String, section: Option[String], titleMessageArgs: Seq[String])(implicit
    messages: Messages
  ): String =
    titleNoForm(s"${errorPrefix(form)} ${messages(titleStr, titleMessageArgs: _*)}", section, Seq())

  private def titleNoForm(title: String, section: Option[String], titleMessageArgs: Seq[String])(implicit
    messages: Messages
  ): String =
    s"${messages(title, titleMessageArgs: _*)}${section.fold(emptyString)(sec => s" - ${messages(sec)}")}"

  private def errorPrefix(form: Form[_])(implicit messages: Messages): String =
    if (form.hasErrors || form.hasGlobalErrors) s"${messages("site.error")}:" else emptyString
}

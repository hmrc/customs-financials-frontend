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

import play.api.data.validation.{Constraint, Invalid, Valid}


trait Constraints {

  lazy val gbnEoriRegex: String = "GBN\\d{11}"
  lazy val eoriRegex: String = "GB\\d{12}"
  lazy val xiEoriRegex: String = "XI\\d{12}"
  lazy val danRegex: String = "^[0-9]{7}"
  lazy val canRegex: String = "^[0-9]{11}"
  lazy val ganRegex: String = "^[a-zA-Z0-9]{8,10}"

  // scalastyle:off cyclomatic.complexity
  protected def checkEORI(invalidFormatErrorKey: String): Constraint[String] =
    Constraint {
      case str if stripWhitespace(str).matches(gbnEoriRegex) => Valid
      case str if stripWhitespace(str).matches(eoriRegex) => Valid
      case str if stripWhitespace(str).matches(danRegex) => Valid
      case str if stripWhitespace(str).matches(canRegex) => Valid
      case str if stripWhitespace(str).matches(ganRegex) => Valid
      case str if stripWhitespace(str).matches(xiEoriRegex) => Valid
      case _ => Invalid(invalidFormatErrorKey, eoriRegex)
    }

  protected def stripWhitespace(str: String): String =
    str.replaceAll("\\s", "").toUpperCase
}

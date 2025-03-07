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
import utils.Utils.emptyString

trait Constraints {

  private lazy val eoriRegex: String    = "^[A-Z]{2}[0-9A-Z]{1,15}$"
  private lazy val gbnEoriRegex: String = "GBN\\d{11}"
  private lazy val gbEoriRegex: String  = "GB\\d{12}"
  lazy val xiEoriRegex: String          = "XI\\d{12}"
  lazy val danRegex: String             = "^[0-9]{7}"
  lazy val canRegex: String             = "^[0-9]{11}"
  lazy val ganRegex: String             = "^[a-zA-Z0-9]{8,10}"

  // scalastyle:off cyclomatic.complexity
  protected def checkEORI(invalidFormatErrorKey: String, isEUEoriEnabled: Boolean = false): Constraint[String] =
    if (isEUEoriEnabled) {
      Constraint {
        case str if stripWhitespace(str).matches(eoriRegex) => Valid
        case _                                              => Invalid(invalidFormatErrorKey, eoriRegex)
      }
    } else {
      Constraint {
        case str if stripWhitespace(str).matches(gbnEoriRegex) => Valid
        case str if stripWhitespace(str).matches(gbEoriRegex)  => Valid
        case str if stripWhitespace(str).matches(danRegex)     => Valid
        case str if stripWhitespace(str).matches(canRegex)     => Valid
        case str if stripWhitespace(str).matches(ganRegex)     => Valid
        case str if stripWhitespace(str).matches(xiEoriRegex)  => Valid
        case _                                                 => Invalid(invalidFormatErrorKey, gbEoriRegex)
      }
    }

  protected def stripWhitespace(str: String): String = str.replaceAll("\\s", emptyString).toUpperCase
}

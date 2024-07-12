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

package views.helpers


import play.api.i18n.Messages
import play.api.test.Helpers
import utils.SpecBase
import utils.TestData.{FILE_SIZE_1000, FILE_SIZE_1000000, FILE_SIZE_42, FILE_SIZE_5430000, FILE_SIZE_999999}

import java.time.{LocalDate, LocalDateTime}

class FormattersTest extends SpecBase {

  implicit val messages: Messages = Helpers.stubMessages()

  "FileFormatters.fileSize" should {
    "Postfix `KB` for files from 1000 to 999999 bytes" in {
      Formatters.fileSize(FILE_SIZE_1000) mustBe "1KB"
      Formatters.fileSize(FILE_SIZE_999999) mustBe "999KB"
    }

    "Postfix `KB` for files less than 1000 bytes and set the value to `1`" in {
      Formatters.fileSize(FILE_SIZE_42) mustBe "1KB"
    }

    "Postfix `MB` for files over 999999 bytes" in {
      Formatters.fileSize(FILE_SIZE_1000000) mustBe "1.0MB"
      Formatters.fileSize(FILE_SIZE_5430000) mustBe "5.4MB"
    }
  }

  "CurrencyFormatters.formatCurrencyAmount" should {
    "format a number to the given number of decimals" in {
      Formatters.formatCurrencyAmount(amount = 999.6565) mustBe "£999.66"
    }

    "omit decimals where the amount is a whole number" in {
      Formatters.formatCurrencyAmount(amount = 999.00) mustBe "£999"
    }

    "include trailing zero if there is any significant decimal place" in {
      Formatters.formatCurrencyAmount(amount = 999.1) mustBe "£999.10"
    }

    "include grouping separator where requested" in {
      Formatters.formatCurrencyAmount(amount = 9999999.99) mustBe "£9,999,999.99"
    }

    "Include plus sign on positive amounts where requested" in {
      Formatters.formatCurrencyAmountWithLeadingPlus(amount = 123456.78) mustBe "+£123,456.78"
    }

    "Omit plus sign where includePlusSign is true but amount is negative" in {
      Formatters.formatCurrencyAmountWithLeadingPlus(amount = -123456.78) mustBe "-£123,456.78"
    }

    "by default" should {
      "format to 2 decimal places, with grouping, for the UK locale" in {
        Formatters.formatCurrencyAmount(amount = 1999.6565) mustBe "£1,999.66"
      }

      "drop zero decimal places" in {
        Formatters.formatCurrencyAmount(amount = 1999.00) mustBe "£1,999"
      }

      "add leading sign to negative amounts" in {
        Formatters.formatCurrencyAmount(amount = -1999.00) mustBe "-£1,999"
      }
    }
  }

  "DateFormatters.dateAsdMMMyyyy" should {

    "format the date using d MMM yyyy pattern (eg. 1 Feb 2020)" in {
      val date = LocalDate.parse("2020-02-01")
      Formatters.dateAsdMMMyyyy(date) mustBe "1 month.abbr.2 2020"
    }
  }

  "DateFormatters.updatedDateTime" should {
    "format the date using hh:mm a on d MMMM yyyy pattern" in {
      val date = LocalDateTime.parse("2020-04-08T12:30")
      Formatters.updatedDateTime(date) mustBe "12:30 pm on 8 month.4 2020"
    }
  }
}

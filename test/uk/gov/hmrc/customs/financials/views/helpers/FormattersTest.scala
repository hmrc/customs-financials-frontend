/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.views.helpers

import play.api.i18n.Messages
import play.api.test.Helpers
utils.SpecBase

import java.time.{LocalDate, LocalDateTime}
import java.util.Locale

class FormattersTest extends SpecBase {
  implicit val messages: Messages = Helpers.stubMessages()
  "FileFormatters.fileSize" should {
    "Postfix `KB` for files from 1000 to 999999 bytes" in {
      Formatters.fileSize(1000) must be ("1KB")
      Formatters.fileSize(999999) must be ("999KB")
    }

    "Postfix `KB` for files less than 1000 bytes and set the value to `1`" in {
      Formatters.fileSize(42) must be ("1KB")
    }

    "Postfix `MB` for files over 999999 bytes" in {
      Formatters.fileSize(1000000) must be ("1.0MB")
      Formatters.fileSize(5430000) must be ("5.4MB")
    }
  }

  "CurrencyFormatters.formatCurrencyAmount" should {
    "format a number to the given number of decimals" in {
      Formatters.formatCurrencyAmount(amount = 999.6565) must be ("£999.66")
    }

    "omit decimals where the amount is a whole number" in {
      Formatters.formatCurrencyAmount(amount = 999.00) must be ("£999")
    }

    "include trailing zero if there is any significant decimal place" in {
      Formatters.formatCurrencyAmount(amount = 999.1) must be ("£999.10")
    }

    "include grouping separator where requested" in {
      Formatters.formatCurrencyAmount(amount = 9999999.99) must be ("£9,999,999.99")
    }

    "Include plus sign on positive amounts where requested" in {
      Formatters.formatCurrencyAmountWithLeadingPlus(amount = 123456.78) must be ("+£123,456.78")
    }

    "Omit plus sign where includePlusSign is true but amount is negative" in {
      Formatters.formatCurrencyAmountWithLeadingPlus(amount = -123456.78) must be ("-£123,456.78")
    }

    "by default" should {
      "format to 2 decimal places, with grouping, for the UK locale" in {
        Formatters.formatCurrencyAmount(amount = 1999.6565) must be ("£1,999.66")
      }

      "drop zero decimal places" in {
        Formatters.formatCurrencyAmount(amount = 1999.00) must be ("£1,999")
      }

      "add leading sign to negative amounts" in {
        Formatters.formatCurrencyAmount(amount = -1999.00) must be ("-£1,999")
      }
    }
  }

  "DateFormatters.dateAsdMMMyyyy" should {

    "format the date using d MMM yyyy pattern (eg. 1 Feb 2020)" in {
      val date = LocalDate.parse("2020-02-01")
      Formatters.dateAsdMMMyyyy(date) must be ("1 month.abbr.2 2020")
    }
  }

  "DateFormatters.timeAsHourMinutesWithAmPm" should {
    "format the date using hh:mm a pattern" in {
      val date = LocalDateTime.parse("2020-04-08T12:30")
      Formatters.timeAsHourMinutesWithAmPm(date) must be ("12:30 PM")
    }
  }

  "DateFormatters.updatedDateTime" should {
    "format the date using hh:mm a on d MMMM yyyy pattern" in {
      val date = LocalDateTime.parse("2020-04-08T12:30")
      Formatters.updatedDateTime(date) must be ("12:30 pm on 8 month.4 2020")
    }
  }
}

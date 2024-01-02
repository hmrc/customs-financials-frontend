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

import java.time.{LocalDate, ZoneId}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import services.DateConverters

import java.text.SimpleDateFormat
import java.util.Date


class DateConvertersSpec extends SpecBase {

  "Date Converters" should {
    "toLocalDate converts datetime to local time" in new Setup {
      private val convert = DateConverters.toLocalDate(date)

      convert mustBe date.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
    }

    "OrderLocalDate returns 0 when passed same day" in new Setup {
      private val convert = DateConverters.OrderedLocalDate(localDate).compare(localDate)

      convert mustBe 0
    }

    "OrderLocalDate returns -1 when passed different date" in new Setup {
      val diffDate: LocalDate = LocalDate.of(year2023, month, day)
      private val convert = DateConverters.OrderedLocalDate(localDate).compare(diffDate)

      convert mustBe -1
    }

    "toJodaTime will return correct date format value yyyy/mm/dd" in new Setup {
      private val convert = DateConverters.toJodaTime(localDate)

      convert.toString mustBe date.toInstant.atZone(ZoneId.systemDefault()).toLocalDate.toString
    }
  }

  trait Setup {
    val year2022 = 2022
    val year2023 = 2023
    val month = 1
    val day = 2

    val date: Date = new SimpleDateFormat("mm/dd/yyyy").parse("01/02/2022")
    val localDate: LocalDate = LocalDate.of(year2022, month, day)
  }
}

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

package services

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import utils.SpecBase

import java.time.{LocalDate, LocalDateTime, LocalTime}

class DateTimeServiceSpec extends SpecBase {

  "return the fixed date if fixedDateTime is enabled" in new Setup {
    val app = application().configure("features.fixed-system-time" -> true).build()
    val service = app.injector.instanceOf[DateTimeService]

    running(app) {
      service.systemDateTime() mustBe
        LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute))
    }
  }

  "return local time .now if fixedDateTime is disabled" in new Setup {
    val app = application().configure("features.fixed-system-time" -> false).build()
    val service = app.injector.instanceOf[DateTimeService]
    val mockClock = mock[DateTimeService]
    def now = LocalDateTime.now()

    when(mockClock.systemDateTime()).thenReturn(now)

    running(app) {
      service.systemDateTime().withNano(zero) mustBe now.withNano(zero)
    }
  }

  trait Setup {
    val year: Int = 2027
    val month: Int = 12
    val day: Int = 20

    val hour: Int = 12
    val minute: Int = 30
    val zero: Int = 0
  }
}

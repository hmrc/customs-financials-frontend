/*
 * Copyright 2022 HM Revenue & Customs
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

package domain

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers._
import utils.SpecBase

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

class EoriHistorySpec extends SpecBase{

  "it" should {
    "parse ISO_LOCAL_DATE" in {
      val eoriHistory = new EoriHistory("GB11111", Some(LocalDate.parse("2019-03-01",DateTimeFormatter.ISO_LOCAL_DATE)),None)
      eoriHistory.validFrom.get.toString mustBe "2019-03-01"
    }

    "parse ISO_INSTANT" in {
      val eoriHistory = new EoriHistory("GB11111", Some(LocalDateTime.parse("1985-03-20T19:30:51Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate),None)
      eoriHistory.validFrom.get.toString mustBe "1985-03-20"
    }

    "obfuscate too short Eori" in {
      obfuscateEori("123") mustBe "123"
    }

    "obfuscate tiny Eori History" in {
      obfuscateEori("1234") mustBe "1234"
    }

    "obfuscate Eori History" in {
      obfuscateEori("12345") mustBe "*2345"
    }
  }
}

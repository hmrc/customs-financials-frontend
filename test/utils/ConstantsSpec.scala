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

import utils.Constants._

class ConstantsSpec extends SpecBase with MustMatchers {

  "FIXED_DATE_TIME_YEAR" should {
    "return correct value" in {
      FIXED_DATE_TIME_YEAR mustBe 2027
    }
  }

  "FIXED_DATE_TIME_MONTH" should {
    "return correct value" in {
      FIXED_DATE_TIME_MONTH mustBe 12
    }
  }

  "FIXED_DATE_TIME_DAY" should {
    "return correct value" in {
      FIXED_DATE_TIME_DAY mustBe 20
    }
  }

  "FIXED_DATE_TIME_HOUR_OF_DAY" should {
    "return correct value" in {
      FIXED_DATE_TIME_HOUR_OF_DAY mustBe 12
    }
  }

  "FIXED_DATE_TIME_MINUTES_OF_HOUR" should {
    "return correct value" in {
      FIXED_DATE_TIME_MINUTES_OF_HOUR mustBe 30
    }
  }
}

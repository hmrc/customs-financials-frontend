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

package controllers

import domain.EoriHistory
import utils.{ShouldMatchers, SpecBase}

import java.time.LocalDate

class OrderedByEoriHistorySpec extends SpecBase with ShouldMatchers {

  "OrderedByEoriHistory" should {

    "correctly compare two instances based on validFrom in EoriHistory" in new Setup {
      val eori1 = "EORI1"
      val eori2 = "EORI2"
      val eoriHistory1: EoriHistory = EoriHistory(eori1, Some(LocalDate.parse("2022-01-01")), None)
      val eoriHistory2: EoriHistory = EoriHistory(eori2, Some(LocalDate.parse("2023-01-01")), None)

      val obj1 = new TestClass(eoriHistory1)
      val obj2 = new TestClass(eoriHistory2)

      obj1.compareTo(obj2) shouldEqual 1
      obj2.compareTo(obj1) should be < 0
      obj1.compareTo(obj1) shouldEqual 0
    }

    "return 1 when validFrom is not available for both instances" in new Setup {
      val eori = "EORI"
      val eoriHistory1: EoriHistory = EoriHistory(eori, None, None)
      val eoriHistory2: EoriHistory = EoriHistory(eori, None, None)

      val obj1 = new TestClass(eoriHistory1)
      val obj2 = new TestClass(eoriHistory2)

      obj1.compareTo(obj2) shouldEqual 1
      obj2.compareTo(obj1) shouldEqual 1
      obj1.compareTo(obj1) shouldEqual 1
    }
  }

  trait Setup {
    class TestClass(override val eoriHistory: EoriHistory) extends OrderedByEoriHistory[TestClass]
  }
}

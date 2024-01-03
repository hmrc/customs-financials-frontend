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

package domain

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsSuccess, Json}
import utils.SpecBase

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

class EoriHistorySpec extends SpecBase {

  "it" should {
    "parse ISO_LOCAL_DATE" in {
      val eoriHistory = new EoriHistory("GB11111",
        Some(LocalDate.parse("2019-03-01", DateTimeFormatter.ISO_LOCAL_DATE)),
        None)

      eoriHistory.validFrom.get.toString mustBe "2019-03-01"
    }

    "parse ISO_INSTANT" in {
      val eoriHistory = new EoriHistory("GB11111",
        Some(LocalDateTime.parse("1985-03-20T19:30:51Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate),
        None)

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

  "isHistoricEori" should {
    "return true" in new Setup {
      eoriHistoryOb.isHistoricEori mustBe true
    }

    "return false" in new Setup {
      eoriHistoryOb.copy(validUntil = None).isHistoricEori mustBe false
    }
  }

  "toLoggable" should {
    "return correct string" in new Setup {
      eoriHistoryOb.toLoggable() mustBe
        s"EoriHistory(Eori: ${obfuscateEori(eori)}, validFrom: Some(2023-10-08), validUntil: Some(2023-10-08))"
    }
  }

  "eoriHistoryWrites" should {
    "return the correct JsValue" in new Setup {
      Json.toJson(eoriHistoryOb) mustBe Json.parse(eoriHistoryObJsonString)
    }
  }

  "reads" should {
    import domain.EoriHistory.eoriHistoryFormat

    "return correct object" when {

      "it has both validFrom and validUntil dates" in new Setup {
        Json.fromJson(Json.parse(eoriHistoryObJsonString)) mustBe JsSuccess(eoriHistoryOb)
      }

      "it has none for validFrom and validUntil dates" in new Setup {
        Json.fromJson(Json.parse(eoriHistoryObWithEmptyDatesJsonString)) mustBe JsSuccess(eoriHistoryObWithEmptyDates)
      }

      "both dates are in ISO_OFFSET_DATE_TIME pattern" in new Setup {
        Json.fromJson(Json.parse(eoriHistoryObWithDatesInISOOffsetPatternJsonString)) mustBe
          JsSuccess(eoriHistoryObWithDatesInISOOffsetPattern)
      }

    }

    "throw exception when dates are in unacceptable format" in new Setup {
      Json.fromJson(Json.parse(eoriHistoryObWithDatesInUnacceptablePatternJsonString)) mustBe
        JsSuccess(eoriHistoryObWithEmptyDates)
    }
  }

  trait Setup {
    val eori = "test_eori"
    val year = 2023
    val month = 10
    val day = 8
    val dateInISOOffsetPatternString = "2011-12-03T10:15:30+01:00"
    val dateInUnacceptableFormatString = "2011-12-03T10:15:30"

    val date: LocalDate = LocalDate.of(year, month, day)
    val dateInISOOffsetPattern: LocalDate =
      LocalDate.parse(dateInISOOffsetPatternString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    val dateInUnacceptablePattern: LocalDate =
      LocalDate.parse(dateInUnacceptableFormatString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    val eoriHistoryOb: EoriHistory = EoriHistory(eori, Some(date), Some(date))
    val eoriHistoryObWithEmptyDates: EoriHistory = EoriHistory(eori, None, None)
    val eoriHistoryObWithDatesInISOOffsetPattern: EoriHistory =
      EoriHistory(eori, Some(dateInISOOffsetPattern), Some(dateInISOOffsetPattern))

    val eoriHistoryObWithDatesInUnacceptablePattern: EoriHistory =
      EoriHistory(eori, Some(dateInUnacceptablePattern), Some(dateInUnacceptablePattern))

    val eoriHistoryObJsonString: String =
      """{"eori":"test_eori","validFrom":"2023-10-08","validUntil":"2023-10-08"}"""".stripMargin

    val eoriHistoryObWithEmptyDatesJsonString: String =
      """{"eori":"test_eori"}"""".stripMargin

    val eoriHistoryObWithDatesInISOOffsetPatternJsonString: String =
      """{
        |"eori":"test_eori",
        |"validFrom":"2011-12-03T10:15:30+01:00",
        |"validUntil":"2011-12-03T10:15:30+01:00"
        |}"""".stripMargin

    val eoriHistoryObWithDatesInUnacceptablePatternJsonString: String =
      """{
        |"eori":"test_eori",
        |"validFrom":"2011-12-03T10:15:30",
        |"validUntil":"2011-12-03T10:15:30"
        |}"""".stripMargin
  }
}

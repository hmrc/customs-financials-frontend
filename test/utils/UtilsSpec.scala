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

import domain.FileFormat.Csv
import domain.FileRole.StandingAuthority
import domain.StandingAuthorityMetadata
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import domain.StandingAuthorityFile
import Utils._

class UtilsSpec extends SpecBase {
  "isSearchQueryAnAccountNumber" should {
    "return true when input string is Account number" in new Setup {
      isSearchQueryAnAccountNumber(seven) mustBe true
      isSearchQueryAnAccountNumber(nine) mustBe true
      isSearchQueryAnAccountNumber("Ab34567890") mustBe true
      isSearchQueryAnAccountNumber("Ab345678") mustBe true
    }

    "return false when input string is an EORI" in {
      isSearchQueryAnAccountNumber("GB1234567789") mustBe false
      isSearchQueryAnAccountNumber("GBN1234567234") mustBe false
      isSearchQueryAnAccountNumber("XI12345670890") mustBe false
    }
  }

  "emptyString" should {
    "return correct value" in {
      emptyString mustBe empty
    }
  }

  "singleSpace" should {
    "return correct value" in {
      singleSpace mustBe " "
    }
  }

  "hyphen" should {
    "return correct value" in {
      hyphen mustBe "-"
    }
  }

  "xiCsvFileNameRegEx" should {
    "return true when string matches the regex" in {
      "SA_XI_000000000154_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
      "SA_XI_00000005666666y153_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
      "SA_XI_avbncgg_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
    }

    "return false when string does not match the regex" in {
      "SA_000000000153_csv.csv".matches(xiCsvFileNameRegEx) mustBe false
      "authorities-2022-11.csv".matches(xiCsvFileNameRegEx) mustBe false
      "TA_000000000154_XI_csv.csv".matches(xiCsvFileNameRegEx) mustBe false
      "SA_000000000156_csv.csv".matches(xiCsvFileNameRegEx) mustBe false
      "_000000000156_csv.csv".matches(xiCsvFileNameRegEx) mustBe false
      "000000000156_csv.csv".matches(xiCsvFileNameRegEx) mustBe false
      "SA_000000000156.csv".matches(xiCsvFileNameRegEx) mustBe false
    }
  }

  "partitionCsvFilesByFileNamePattern" should {
    "return correct list of GB and XI authorities partitioned by the file name pattern" in new Setup {
      val standAuthMetadata: StandingAuthorityMetadata =
        StandingAuthorityMetadata(year, month, day, Csv, StandingAuthority)

      val gbAuthFiles = Seq(
        StandingAuthorityFile("SA_000000000153_csv.csv", "", size, standAuthMetadata, "GB123456789012"),
        StandingAuthorityFile("SA_000000000154_csv.csv", "", size, standAuthMetadata, "GB123456789012"))

      val xiAuthFiles =
        Seq(StandingAuthorityFile("SA_XI_000000000153_csv.csv", "", size, standAuthMetadata, "XI123456789012"),
          StandingAuthorityFile("SA_XI_000000000154_csv.csv", "", size, standAuthMetadata, "XI123456789012"))

      val csvFileForBothGBAndXI = gbAuthFiles ++ xiAuthFiles

      partitionCsvFilesByFileNamePattern(csvFileForBothGBAndXI) mustBe
        CsvFiles(gbCsvFiles = gbAuthFiles, xiCsvFiles = xiAuthFiles)

      partitionCsvFilesByFileNamePattern(gbAuthFiles) mustBe CsvFiles(
        gbCsvFiles = gbAuthFiles, xiCsvFiles = Seq.empty)

      partitionCsvFilesByFileNamePattern(xiAuthFiles) mustBe CsvFiles(
        gbCsvFiles = Seq.empty, xiCsvFiles = xiAuthFiles)
    }

    "return empty list of GB and XI authorities partitioned when input list is empty" in {
      partitionCsvFilesByFileNamePattern(Seq.empty) mustBe CsvFiles(Seq.empty, Seq.empty)
    }
  }

  "xiEoriRegex" should {
    "return true when input matches regex" in {
      "XI123456789012".matches(xiEoriRegex) mustBe true
      "XI567896345987".matches(xiEoriRegex) mustBe true
      "XI888888888888".matches(xiEoriRegex) mustBe true
      "XI000000000000".matches(xiEoriRegex) mustBe true
      "XI000000000000".matches(xiEoriRegex) mustBe true
    }

    "return false when input does not match regex" in {
      "xi123456789012".matches(xiEoriRegex) mustBe false
      "XI567896345".matches(xiEoriRegex) mustBe false
      "56789634512345".matches(xiEoriRegex) mustBe false
      "5678963451XI23".matches(xiEoriRegex) mustBe false
      "567896345123XI".matches(xiEoriRegex) mustBe false
      emptyString.matches(xiEoriRegex) mustBe false
      "GB123456789012".matches(xiEoriRegex) mustBe false
      "gb123456789012".matches(xiEoriRegex) mustBe false
      "GB12345678".matches(xiEoriRegex) mustBe false
      "567896345123GB".matches(xiEoriRegex) mustBe false
      "GBN12345678901".matches(xiEoriRegex) mustBe false
      "GBN123456789".matches(xiEoriRegex) mustBe false
    }
  }

  "isXIEori" should {
    "return true when input is XI EORI" in {
      isXIEori("XI123456789012") mustBe true
      isXIEori("XI567896345987") mustBe true
      isXIEori("XI888888888888") mustBe true
      isXIEori("XI000000000000") mustBe true
      isXIEori("XI000000000000  ") mustBe true
      isXIEori(" XI000000000000  ") mustBe true
    }

    "return false when input is not XI EORI" in {
      isXIEori("xi123456789012") mustBe false
      isXIEori("XI567896345") mustBe false
      isXIEori("56789634512345") mustBe false
      isXIEori("5678963451XI23") mustBe false
      isXIEori("567896345123XI") mustBe false
      isXIEori(emptyString) mustBe false
      isXIEori("GB123456789012") mustBe false
      isXIEori("gb123456789012") mustBe false
      isXIEori("GB12345678") mustBe false
      isXIEori("567896345123GB") mustBe false
      isXIEori("GBN12345678901") mustBe false
      isXIEori("GBN123456789") mustBe false
    }
  }

  trait Setup {
    val seven: String = "1234567"
    val nine: String = "123456789"

    val year: Int = 2022
    val month: Int = 6
    val day: Int = 1

    val size = 500L
  }
}

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
  "return true when input string is Account number" in {
    Utils.isSearchQueryAnAccountNumber("1234567") mustBe true
    Utils.isSearchQueryAnAccountNumber("123456789") mustBe true
    Utils.isSearchQueryAnAccountNumber("Ab34567890") mustBe true
    Utils.isSearchQueryAnAccountNumber("Ab345678") mustBe true
  }

  "return false when input string is an EORI" in {
    Utils.isSearchQueryAnAccountNumber("GB1234567789") mustBe false
    Utils.isSearchQueryAnAccountNumber("GBN1234567234") mustBe false
    Utils.isSearchQueryAnAccountNumber("XI12345670890") mustBe false
  }
}

  "emptyString" should {
    "return correct value" in {
      Utils.emptyString mustBe empty
    }
  }

  "xiCsvFileNameRegEx" should {
    "return true when string matches the regex" in {
      "SA_000000000154_XI_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
      "SA_00000005666666y153_XI_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
      "SA_avbncgg_XI_csv.csv".matches(xiCsvFileNameRegEx) mustBe true
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
    "return correct list of GB and XI authorities partitioned by the file name pattern" in {
      val standAuthMetadata: StandingAuthorityMetadata =
        StandingAuthorityMetadata(2022, 6, 1, Csv, StandingAuthority)

      val gbAuthFiles = Seq(
        StandingAuthorityFile("SA_000000000153_csv.csv", "", 500L, standAuthMetadata, "GB123456789012"),
        StandingAuthorityFile("SA_000000000154_csv.csv", "", 500L, standAuthMetadata, "GB123456789012"))

      val xiAuthFiles =
        Seq(StandingAuthorityFile("SA_000000000153_XI_csv.csv", "", 500L, standAuthMetadata, "XI123456789012"),
          StandingAuthorityFile("SA_000000000154_XI_csv.csv", "", 500L, standAuthMetadata, "XI123456789012"))

      val csvFileForBothGBAndXI = gbAuthFiles ++ xiAuthFiles

      Utils.partitionCsvFilesByFileNamePattern(csvFileForBothGBAndXI) mustBe
        CsvFiles(gbCsvFiles = gbAuthFiles, xiCsvFiles = xiAuthFiles)

      Utils.partitionCsvFilesByFileNamePattern(gbAuthFiles) mustBe CsvFiles(
        gbCsvFiles = gbAuthFiles, xiCsvFiles = Seq.empty)

      Utils.partitionCsvFilesByFileNamePattern(xiAuthFiles) mustBe CsvFiles(
        gbCsvFiles = Seq.empty, xiCsvFiles = xiAuthFiles)
    }

    "return empty list of GB and XI authorities partitioned when input list is empty" in {

      Utils.partitionCsvFilesByFileNamePattern(Seq.empty) mustBe CsvFiles(Seq.empty, Seq.empty)
    }
  }
}

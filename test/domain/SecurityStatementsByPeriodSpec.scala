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

import domain.FileFormat.Pdf
import domain.FileRole.SecurityStatement

import utils.SpecBase

import java.time.LocalDate

class SecurityStatementsByPeriodSpec extends SpecBase {

  "pdf" should {
    "return the file when files has pdf format" in new Setup {
      securityStatPeriod1.pdf mustBe Some(securityStatFile)
    }

    "return None when there are no pdf format" in new Setup {
      securityStatPeriod1.copy(files = Seq()).pdf mustBe empty
    }
  }

  "compare" should {
    "sort correctly" in new Setup {
      List(securityStatPeriod2, securityStatPeriod1).sorted mustBe List(securityStatPeriod1, securityStatPeriod2)
    }
  }

  trait Setup {
    val startYear = 2023
    val endYear = 2023

    val year = 2023
    val month = 10
    val day = 8
    val eori = "test_eori"

    val fileName = "test_file"
    val downloadUrl = "test_url"
    val size = 2048

    val date: LocalDate = LocalDate.of(year, month, day)

    val metadata: SecurityStatementFileMetadata = SecurityStatementFileMetadata(
      startYear, month, day, endYear, month, day, Pdf, SecurityStatement, eori, size, "checksum", None)

    val securityStatFile: SecurityStatementFile = SecurityStatementFile(fileName, downloadUrl, size, metadata)

    val securityStatPeriod1: SecurityStatementsByPeriod =
      SecurityStatementsByPeriod(date, date.plusDays(1), Seq(securityStatFile))

    val securityStatPeriod2: SecurityStatementsByPeriod =
      SecurityStatementsByPeriod(date.plusDays(2), date.plusDays(3), Seq(securityStatFile))
  }
}

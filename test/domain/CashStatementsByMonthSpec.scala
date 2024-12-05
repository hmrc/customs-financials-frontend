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

import domain.FileFormat.{Csv, Pdf}
import domain.FileRole.SecurityStatement
import play.api.Application
import play.api.i18n.Messages
import utils.{MustMatchers, SpecBase}

import java.time.LocalDate

class CashStatementsByMonthSpec extends SpecBase with MustMatchers {

  "formattedMonth" should {
    "return correct formatted value for the month" in new Setup {
      cashStatementByMonthWithPdf.formattedMonth mustBe "October"
    }
  }

  "formattedMonthYear" should {
    "return correct formatted Month and year string" in new Setup {
      cashStatementByMonthWithPdf.formattedMonthYear mustBe "October 2023"
    }
  }

  "pdf" should {
    "return CashStatementFile when files has pdf format" in new Setup {
      cashStatementByMonthWithPdf.pdf mustBe Some(cashStatementFilePdf)
    }

    "return None when files has no file with pdf format" in new Setup {
      cashStatementByMonthWithCsv.pdf mustBe empty
    }
  }

  "csv" should {
    "return CashStatementFile when files has csv format" in new Setup {
      cashStatementByMonthWithCsv.csv mustBe Some(cashStatementFileCsv)
    }

    "return None when files has no file with csv format" in new Setup {
      cashStatementByMonthWithPdf.csv mustBe empty
    }
  }

  "compare" should {
    "sort correctly" in new Setup {
      List(cashStatementByMonthWithCsv, cashStatementByMonthWithPdf).sorted mustBe List(
        cashStatementByMonthWithPdf,
        cashStatementByMonthWithCsv
      )
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

    val app: Application = application().build()
    implicit val msgs: Messages = messages(app)

    val metadataWithPdf: CashStatementFileMetadata =
      CashStatementFileMetadata(startYear, month, Pdf, SecurityStatement, None)

    val metadataWithCsv: CashStatementFileMetadata =
      CashStatementFileMetadata(startYear, month, Csv, SecurityStatement, None)

    val cashStatementFilePdf: CashStatementFile = CashStatementFile(
      fileName,
      downloadUrl,
      size,
      metadataWithPdf,
      eori
    )

    val cashStatementFileCsv: CashStatementFile = CashStatementFile(fileName, downloadUrl, size, metadataWithCsv, eori)

    val cashStatementByMonthWithPdf: CashStatementsByMonth =
      CashStatementsByMonth(date, Seq(cashStatementFilePdf))

    val cashStatementByMonthWithCsv: CashStatementsByMonth =
      CashStatementsByMonth(date.plusMonths(1), Seq(cashStatementFileCsv))
  }
}

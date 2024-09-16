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

class CashAccountStatementByMonthSpec extends SpecBase with MustMatchers {

  "formattedMonth" should {
    "return correct formatted value for the month" in new Setup {
      cashAccountStatementByMonthWithPdf.formattedMonth mustBe "October"
    }
  }

  "formattedMonthYear" should {
    "return correct formatted Month and year string" in new Setup {
      cashAccountStatementByMonthWithPdf.formattedMonthYear mustBe "October 2023"
    }
  }

  "pdf" should {
    "return CashAccountStatementFile when files has pdf format" in new Setup {
      cashAccountStatementByMonthWithPdf.pdf mustBe Some(cashAccountStatementFilePdf)
    }

    "return None when files has no file with pdf format" in new Setup {
      cashAccountStatementByMonthWithCsv.pdf mustBe empty
    }
  }

  "csv" should {
    "return CashAccountStatementFile when files has csv format" in new Setup {
      cashAccountStatementByMonthWithCsv.csv mustBe Some(cashAccountStatementFileCsv)
    }

    "return None when files has no file with csv format" in new Setup {
      cashAccountStatementByMonthWithPdf.csv mustBe empty
    }
  }

  "compare" should {
    "sort correctly" in new Setup {
      List(cashAccountStatementByMonthWithCsv,
        cashAccountStatementByMonthWithPdf
      ).sorted mustBe List(cashAccountStatementByMonthWithPdf, cashAccountStatementByMonthWithCsv)
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

    val metadataWithPdf: CashAccountStatementFileMetadata = CashAccountStatementFileMetadata(
      startYear, month, Pdf, SecurityStatement, None)

    val metadataWithCsv: CashAccountStatementFileMetadata = CashAccountStatementFileMetadata(
      startYear, month, Csv, SecurityStatement, None)

    val cashAccountStatementFilePdf: CashAccountStatementFile = CashAccountStatementFile(
      fileName,
      downloadUrl,
      size,
      metadataWithPdf,
      eori
    )

    val cashAccountStatementFileCsv: CashAccountStatementFile = CashAccountStatementFile(fileName,
      downloadUrl,
      size,
      metadataWithCsv,
      eori
    )

    val cashAccountStatementByMonthWithPdf: CashAccountStatementsByMonth =
      CashAccountStatementsByMonth(date, Seq(cashAccountStatementFilePdf))

    val cashAccountStatementByMonthWithCsv: CashAccountStatementsByMonth =
      CashAccountStatementsByMonth(date.plusMonths(1), Seq(cashAccountStatementFileCsv))
  }
}

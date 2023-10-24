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

import domain.DDStatementType.{Excise, Supplementary, UnknownStatementType, Weekly}
import domain.FileFormat.{Csv, Pdf, UnknownFileFormat}
import domain.FileRole.{C79Certificate, DutyDefermentStatement, PostponedVATAmendedStatement, PostponedVATStatement, SecurityStatement, StandingAuthority}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import utils.SpecBase
import views.helpers.Formatters

import java.time.LocalDate
import scala.util.Random

class SdesFileSpec extends SpecBase {

  "an SdesFile" should {

    "be correctly ordered" in new Setup() {

      val metadata = randomDutyDefermentStatementFile(1).metadata
      val pdf = randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Pdf))
      val csv = randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Csv))

      List(csv,pdf).sorted.map(_.metadata).map(_.fileFormat) mustBe List(Pdf, Csv)
      List(pdf,csv).sorted.map(_.metadata).map(_.fileFormat) mustBe List(Pdf, Csv)
    }
  }

  "FileRole" should {
    "return correct output for apply method" in new Setup {
      FileRole("DutyDefermentStatement") mustBe DutyDefermentStatement
      FileRole("C79Certificate") mustBe C79Certificate
      FileRole("PostponedVATStatement") mustBe PostponedVATStatement
      FileRole("PostponedVATAmendedStatement") mustBe PostponedVATAmendedStatement
      FileRole("SecurityStatement") mustBe SecurityStatement
      FileRole("StandingAuthority") mustBe StandingAuthority

      intercept[Exception] {
        FileRole("Unknown")
      }
    }

    "return correct output for unapply method" in new Setup {
      FileRole.unapply(FileRole("DutyDefermentStatement")) mustBe Some("DutyDefermentStatement")
    }
  }

  "DDStatementType" should {
    "return correct value for apply method" in new Setup {
      DDStatementType("Excise") mustBe Excise
      DDStatementType("Supplementary") mustBe Supplementary
      DDStatementType("Weekly") mustBe Weekly
      DDStatementType("UNKNOWN STATEMENT TYPE") mustBe UnknownStatementType
    }

    "return correct value for unapply method" in new Setup {
      DDStatementType.unapply(DDStatementType("Excise")) mustBe Some("Excise")
    }

    "provide correct output while comparing" in new Setup {
      List(DDStatementType("Excise"), DDStatementType("Supplementary")).min.name mustBe
        "Excise"
    }
  }

  "SecurityStatementFile" should {
    "return correct output for startDate, endDate and formattedSize" in new Setup {

      val secStatFile: SecurityStatementFile = SecurityStatementFile("test_file_name",
        "test_url",
        2064L,
        secureMetaData)

      secStatFile.startDate mustBe LocalDate.of(
        secureMetaData.periodStartYear,
        secureMetaData.periodStartMonth,
        secureMetaData.periodStartDay)

      secStatFile.endDate mustBe LocalDate.of(
        secureMetaData.periodEndYear,
        secureMetaData.periodEndMonth,
        secureMetaData.periodEndDay)

      secStatFile.formattedSize mustBe Formatters.fileSize(1234L)
    }
  }

  "FileFormat" should {
    "return correct value for apply method" in new Setup {
      FileFormat("PDF") mustBe Pdf
      FileFormat("CSV") mustBe Csv
      FileFormat("UNKNOWN FILE FORMAT") mustBe UnknownFileFormat
    }

    "return correct value for unapply method" in new Setup {
      FileFormat.unapply(FileFormat("PDF")) mustBe Some("PDF")
    }
  }

  "VatCertificateFile" should {
    "return correct output for formattedSize" in new Setup {
      vatCertFile.formattedSize mustBe Formatters.fileSize(2164L)

    }

    "return correct output for formattedMonth" in new Setup {
      vatCertFile.formattedMonth mustBe "month.1"
    }
  }

  trait Setup {
    implicit val msg: Messages = stubMessages()

    val secureMetaData: SecurityStatementFileMetadata =
      SecurityStatementFileMetadata(1972, 2, 20, 2010, 1, 2, FileFormat.Csv, FileRole.SecurityStatement,
        "GB1234567890", 1234L, "check it", Some("thing"))

    val vatCertificateFileMetadata: VatCertificateFileMetadata =
      VatCertificateFileMetadata(2010, 1, FileFormat.Csv, FileRole.C79Certificate, None)

    val vatCertFile: VatCertificateFile = VatCertificateFile("test_file_name",
      "test_url",
      2164L,
      vatCertificateFileMetadata,
      "test_eori")

    def randomInt(limit: Int) = Random.nextInt(limit)
    def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

    def randomDutyDefermentStatementFile(size: Long): DutyDefermentStatementFile = DutyDefermentStatementFile(
      s"${randomString(8)}.${randomString(3)}",
      s"http://${randomString(8)}.com/",
      size,
      DutyDefermentStatementFileMetadata(randomInt(2017) + 1, randomInt(11) + 1, randomInt(27) + 1,
        randomInt(2017) + 1, randomInt(11) + 1, randomInt(27) + 1, Pdf, DutyDefermentStatement,
        Weekly, Some(true), Some("BACS"), s"${randomInt(8)}", None)
    )
  }

}

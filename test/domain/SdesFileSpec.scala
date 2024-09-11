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
import domain.DutyPaymentMethod.CDS
import domain.FileFormat.{Csv, Pdf, UnknownFileFormat}
import domain.FileRole._
import play.api.i18n.Messages
import play.api.libs.json.{JsString, JsSuccess, Json}
import play.api.test.Helpers.stubMessages
import utils.SpecBase
import utils.TestData._
import views.helpers.Formatters
import utils.MustMatchers

import java.time.LocalDate
import scala.util.Random

class SdesFileSpec extends SpecBase with MustMatchers {

  "an SdesFile" should {

    "be correctly ordered" in new Setup() {

      val metadata: DutyDefermentStatementFileMetadata = randomDutyDefermentStatementFile(1).metadata
      val pdf: DutyDefermentStatementFile =
        randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Pdf))

      val csv: DutyDefermentStatementFile =
        randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Csv))

      List(csv, pdf).sorted.map(_.metadata).map(_.fileFormat) mustBe List(Pdf, Csv)
      List(pdf, csv).sorted.map(_.metadata).map(_.fileFormat) mustBe List(Pdf, Csv)
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
      FileRole("CashAccountStatement") mustBe CashAccountStatement

      intercept[Exception] {
        FileRole("Unknown")
      }
    }

    "return correct output for unapply method" in new Setup {
      val fileRoleName: String = FileRole("DutyDefermentStatement") match {
        case FileRole(name) => name
        case _ => emptyString
      }

      fileRoleName mustBe "DutyDefermentStatement"
    }

    "return correct output for Reads" in new Setup {

      import domain.FileRole.fileRoleFormat

      Json.fromJson(JsString("C79Certificate")) mustBe JsSuccess(FileRole("C79Certificate"))
      Json.fromJson(JsString("DutyDefermentStatement")) mustBe JsSuccess(FileRole("DutyDefermentStatement"))
      Json.fromJson(JsString("PostponedVATStatement")) mustBe JsSuccess(FileRole("PostponedVATStatement"))
      Json.fromJson(JsString("PostponedVATAmendedStatement")) mustBe JsSuccess(FileRole("PostponedVATAmendedStatement"))
      Json.fromJson(JsString("SecurityStatement")) mustBe JsSuccess(FileRole("SecurityStatement"))
      Json.fromJson(JsString("StandingAuthority")) mustBe JsSuccess(FileRole("StandingAuthority"))
      Json.fromJson(JsString("CashAccountStatement")) mustBe JsSuccess(FileRole("CashAccountStatement"))
    }

    "return correct output for Writes" in new Setup {
      Json.toJson(FileRole("C79Certificate")) mustBe JsString("C79Certificate")
    }

    "bind correct value for pathBinder" in {
      import domain.FileRole.pathBinder

      pathBinder.bind(emptyString, "import-vat") mustBe Right(C79Certificate)
      pathBinder.bind(emptyString, "postponed-vat") mustBe Right(PostponedVATStatement)
      pathBinder.bind(emptyString, "duty-deferment") mustBe Right(DutyDefermentStatement)
      pathBinder.bind(emptyString, "adjustments") mustBe Right(SecurityStatement)
      pathBinder.bind(emptyString, "authorities") mustBe Right(StandingAuthority)
      pathBinder.bind(emptyString, "cash-account-statement") mustBe Right(CashAccountStatement)
      pathBinder.bind(emptyString, "unknown") mustBe Left(s"unknown file role: unknown")
    }

    "unbind correct value for pathBinder" in {
      import domain.FileRole.pathBinder

      pathBinder.unbind(emptyString, C79Certificate) mustBe "import-vat"
      pathBinder.unbind(emptyString, PostponedVATStatement) mustBe "postponed-vat"
      pathBinder.unbind(emptyString, DutyDefermentStatement) mustBe "duty-deferment"
      pathBinder.unbind(emptyString, SecurityStatement) mustBe "adjustments"
      pathBinder.unbind(emptyString, StandingAuthority) mustBe "authorities"
      pathBinder.unbind(emptyString, CashAccountStatement) mustBe "cash-account-statement"
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
      val exciseTypeName: String = DDStatementType("Excise") match {
        case DDStatementType(name) => name
        case _ => emptyString
      }

      exciseTypeName mustBe "Excise"
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
        FILE_SIZE_2064,
        secureMetaData)

      secStatFile.startDate mustBe LocalDate.of(
        secureMetaData.periodStartYear,
        secureMetaData.periodStartMonth,
        secureMetaData.periodStartDay)

      secStatFile.endDate mustBe LocalDate.of(
        secureMetaData.periodEndYear,
        secureMetaData.periodEndMonth,
        secureMetaData.periodEndDay)

      secStatFile.formattedSize mustBe Formatters.fileSize(FILE_SIZE_DEFAULT)
    }

    "sort correctly" in new Setup {
      val secStatFile1: SecurityStatementFile = SecurityStatementFile(fileName,
        downloadUrl,
        size,
        secureMetaData)

      val secStatFile2: SecurityStatementFile = SecurityStatementFile(fileName,
        downloadUrl,
        size,
        secureMetaData.copy(periodStartYear = secureMetaData.periodStartYear + 1))

      List(secStatFile2, secStatFile1).sorted mustBe List(secStatFile1, secStatFile2)
    }
  }

  "FileFormat" should {
    "return correct value for apply method" in new Setup {
      FileFormat("PDF") mustBe Pdf
      FileFormat("CSV") mustBe Csv
      FileFormat("UNKNOWN FILE FORMAT") mustBe UnknownFileFormat
    }

    "return correct value for unapply method" in new Setup {
      val pdfFileFormatName: String = FileFormat("PDF") match {
        case FileFormat(name) => name
        case _ => emptyString
      }

      pdfFileFormatName mustBe "PDF"
    }
  }

  "VatCertificateFile" should {
    "return correct output for formattedSize" in new Setup {
      vatCertFile.formattedSize mustBe Formatters.fileSize(FILE_SIZE_2164)
    }

    "return correct output for formattedMonth" in new Setup {
      vatCertFile.formattedMonth mustBe "month.1"
    }

    "sort the files correctly" in new Setup {
      val vatCerFile1: VatCertificateFile = VatCertificateFile(fileName,
        downloadUrl,
        size,
        vatCerMetaData,
        eori)

      val vatCerFile2: VatCertificateFile = VatCertificateFile(fileName,
        downloadUrl,
        size,
        vatCerMetaData.copy(periodStartYear = secureMetaData.periodStartYear + 1, fileFormat = Csv),
        eori)

      List(vatCerFile1, vatCerFile2).sorted mustBe List(vatCerFile2, vatCerFile1)
    }
  }

  "StandingAuthorityFile" should {
    "sort the files correctly" in new Setup {
      val standAuthFile1: StandingAuthorityFile = StandingAuthorityFile(fileName,
        downloadUrl,
        size,
        standAuthMetaData,
        eori)

      val standAuthFile2: StandingAuthorityFile = StandingAuthorityFile(fileName,
        downloadUrl,
        size,
        standAuthMetaData.copy(periodStartYear = standAuthMetaData.periodStartYear + 1),
        eori)

      List(standAuthFile2, standAuthFile1).sorted mustBe List(standAuthFile1, standAuthFile2)
    }
  }

  "PostponedVatStatementFile" should {
    "sort the files correctly" in new Setup {
      val pVatStatFile1: PostponedVatStatementFile = PostponedVatStatementFile(fileName,
        downloadUrl,
        size,
        pVatStataMetaData,
        eori)

      val pVatStatFile2: PostponedVatStatementFile = PostponedVatStatementFile(fileName,
        downloadUrl,
        size,
        pVatStataMetaData.copy(periodStartYear = pVatStataMetaData.periodStartYear + 1, fileFormat = Csv),
        eori)

      List(pVatStatFile1, pVatStatFile2).sorted mustBe List(pVatStatFile2, pVatStatFile1)
    }
  }

  "FileFormat" should {
    "generate correct output" when {
      "reads" in new Setup {

        import FileFormat.fileFormatFormat

        Json.fromJson(JsString("PDF")) mustBe JsSuccess(Pdf)
      }

      "writes" in new Setup {
        Json.toJson[FileFormat](Pdf) mustBe JsString(Pdf.name)
      }
    }
  }

  trait Setup {
    implicit val msg: Messages = stubMessages()

    val fileName = "test_file"
    val downloadUrl = "test_url"
    val size = 2064L

    val startYear = 2021
    val month = 10
    val day = 2
    val eori = "test_eori"

    val secureMetaData: SecurityStatementFileMetadata =
      SecurityStatementFileMetadata(YEAR_1972, MONTH_2, DAY_20, YEAR_2010, MONTH_1, DAY_2, Csv,
        FileRole.SecurityStatement, "GB1234567890", FILE_SIZE_DEFAULT, "check it", Some("thing"))

    val standAuthMetaData: StandingAuthorityMetadata =
      StandingAuthorityMetadata(startYear, month, day, Pdf, StandingAuthority)

    val vatCerMetaData: VatCertificateFileMetadata =
      VatCertificateFileMetadata(startYear, month, Pdf, C79Certificate, None)

    val pVatStataMetaData: PostponedVatStatementFileMetadata =
      PostponedVatStatementFileMetadata(startYear, month, Pdf, C79Certificate, CDS, None)

    val vatCertificateFileMetadata: VatCertificateFileMetadata =
      VatCertificateFileMetadata(YEAR_2010, MONTH_1, Csv, FileRole.C79Certificate, None)

    val vatCertFile: VatCertificateFile = VatCertificateFile("test_file_name",
      "test_url",
      FILE_SIZE_2164,
      vatCertificateFileMetadata,
      "test_eori")

    def randomInt(limit: Int): Int = Random.nextInt(limit)

    def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

    def randomDutyDefermentStatementFile(size: Long): DutyDefermentStatementFile = DutyDefermentStatementFile(
      s"${randomString(LENGTH_8)}.${randomString(3)}",
      s"http://${randomString(LENGTH_8)}.com/",
      size,
      DutyDefermentStatementFileMetadata(randomInt(YEAR_2017) + 1, randomInt(LENGTH_11) + 1, randomInt(LENGTH_27) + 1,
        randomInt(YEAR_2017) + 1, randomInt(LENGTH_11) + 1, randomInt(LENGTH_27) + 1, Pdf, DutyDefermentStatement,
        Weekly, Some(true), Some("BACS"), s"${randomInt(LENGTH_8)}", None)
    )
  }

}

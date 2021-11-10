/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.customs.financials.domain

utils.SpecBase

//scalastyle:off magic.number
class DownloadStatementAuditDataSpec extends SpecBase {

  "DownloadStatementAuditDataSpec.apply" should {

    "correctly map DutyDefermentStatementFileMetadata and include the eori" in {
      val dutyDefermentStatementFileMetadata = DutyDefermentStatementFileMetadata(
        1972, 2, 20, 2010, 1, 2,
        FileFormat.Pdf, FileRole.DutyDefermentStatement, DDStatementType.Weekly,
        Some(true), Some("GreenShieldStamps"), "12345678", Some("some request id")
      )

      val sut = DownloadStatementAuditData(dutyDefermentStatementFileMetadata, "12345")
      sut.auditData mustBe Map(
        "eori" -> "12345",
        "periodStartYear" -> "1972",
        "periodStartMonth" -> "2",
        "periodStartDay" -> "20",
        "periodEndYear" -> "2010",
        "periodEndMonth" -> "1",
        "periodEndDay" -> "2",
        "fileFormat" -> "PDF",
        "fileRole" -> "DutyDefermentStatement",
        "defermentStatementType" -> "Weekly",
        "dutyOverLimit" -> "Some(true)",
        "dutyPaymentType" -> "Some(GreenShieldStamps)",
        "dan" -> "12345678",
        "statementRequestId" -> "Some(some request id)"
      )
    }

    "correctly map VatCertificateFileMetadata and include the eori" in {
      val vatCertificateFileMetadata = VatCertificateFileMetadata(
        2010, 1, FileFormat.Csv, FileRole.C79Certificate, None
      )

      val sut = DownloadStatementAuditData(vatCertificateFileMetadata, "12345")
      sut.auditData mustBe Map(
        "eori" -> "12345",
        "periodStartYear" -> "2010",
        "periodStartMonth" -> "1",
        "fileFormat" -> "CSV",
        "fileRole" -> "C79Certificate",
        "statementRequestId" -> "None"
      )
    }

    "correctly map PostponedVatCertificateFileMetadata and include the eori" in {
      val postponedVatCertificateFileMetadata = PostponedVatStatementFileMetadata(
        2010, 2, FileFormat.Pdf, FileRole.PostponedVATStatement, "some HoDs", Some("some request Id")
      )

      val sut = DownloadStatementAuditData(postponedVatCertificateFileMetadata, "12345")
      sut.auditData mustBe Map(
        "eori" -> "12345",
        "periodStartYear" -> "2010",
        "periodStartMonth" -> "2",
        "fileFormat" -> "PDF",
        "fileRole" -> "PostponedVATStatement",
        "source" -> "some HoDs",
        "statementRequestId" -> "Some(some request Id)"
      )
    }

    "correctly map SecurityStatementFileMetadata and include the eori" in {
      val securityStatementFileMetadata = SecurityStatementFileMetadata(
        1972, 2, 20, 2010, 1, 2, FileFormat.Csv, FileRole.SecurityStatement,
        "GB1234567890", 1234L, "check it", Some("thing")
      )

      val sut = DownloadStatementAuditData(securityStatementFileMetadata, "12345")
      sut.auditData mustBe Map(
        "eori" -> "12345",
        "periodStartYear" -> "1972",
        "periodStartMonth" -> "2",
        "periodStartDay" -> "20",
        "periodEndYear" -> "2010",
        "periodEndMonth" -> "1",
        "periodEndDay" -> "2",
        "fileFormat" -> "CSV",
        "fileRole" -> "SecurityStatement",
        "eoriNumber" -> "GB1234567890",
        "fileSize" -> "1234",
        "checksum" -> "check it",
        "statementRequestId" -> "Some(thing)"
      )
    }
  }
}

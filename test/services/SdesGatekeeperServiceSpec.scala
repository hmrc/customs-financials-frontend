/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import domain.DutyPaymentMethod.CDS
import domain.FileFormat.{Csv, Pdf}
import domain.FileRole.{C79Certificate, PostponedVATStatement, SecurityStatement, StandingAuthority}
import domain.{Metadata, MetadataItem, PostponedVatStatementFile, PostponedVatStatementFileMetadata, SecurityStatementFile, SecurityStatementFileMetadata, StandingAuthorityFile, StandingAuthorityMetadata, VatCertificateFile, VatCertificateFileMetadata}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.SpecBase


//scalastyle:off magic.number
class SdesGatekeeperServiceSpec extends SpecBase {
  implicit val messages: Messages = Helpers.stubMessages()
  "SdesGatekeeperService" should {

    "convertToVatCertificateFile" should {

      "create VatCertificateFile from FileInformation" in  {
        val metadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("FileType", "PDF"),
          MetadataItem("FileRole", "C79Certificate")
        )

        val fileInformation = domain.FileInformation(
          "C79_100000000323_pdf.pdf",
          "https://some.sdes.domain?token=abc123",
          1234,
          Metadata(metadata)
        )
        val expectedVatCertificateFile = VatCertificateFile(
          "C79_100000000323_pdf.pdf",
          "https://some.sdes.domain?token=abc123",
          1234L,
          VatCertificateFileMetadata(2018, 6, Pdf, C79Certificate, None),
          ""
        )
        val sdesGatekeeperService = new SdesGatekeeperService()

        val vatCertificateFile = sdesGatekeeperService.convertToVatCertificateFile(fileInformation)

        vatCertificateFile must be(expectedVatCertificateFile)
      }

      "raise exception if mandatory information is missing" in {
        val metadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("FileType", "PDF"),
          MetadataItem("FileRole", "C79Certificate")
        )
        val sdesGatekeeperService = new SdesGatekeeperService()
        metadata.foreach { item =>

          val incompleteMetadata = Metadata(metadata.filterNot(_ == item))

          assertThrows[NoSuchElementException] {

            val fileInformation = domain.FileInformation(
              "C79_100000000323_pdf.pdf",
              "https://some.sdes.domain?token=abc123",
              1234L,
              incompleteMetadata
            )

            sdesGatekeeperService.convertToVatCertificateFile(fileInformation)
          }
        }
      }
    }

    "convertToPostponedVatCertificateFile" should {

      "create PostponedVatCertificateFile from FileInformation" in {
        val sdesGatekeeperService = new SdesGatekeeperService()

        val metadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("FileType", "PDF"),
          MetadataItem("FileRole", "PostponedVATStatement"),
          MetadataItem("DutyPaymentMethod", "Immediate")
        )

        val fileInformation = domain.FileInformation(
          "PostponedVATStatement_100000000323_pdf.pdf",
          "https://some.sdes.domain?token=abc123",
          1234,
          Metadata(metadata)
        )
        val expectedPostponedVatCertificateFile = PostponedVatStatementFile(
          "PostponedVATStatement_100000000323_pdf.pdf",
          "https://some.sdes.domain?token=abc123",
          1234L,
          PostponedVatStatementFileMetadata(2018, 6, Pdf, PostponedVATStatement, CDS, None),
          ""
        )

        val postponedVatCertificateFile = sdesGatekeeperService.convertToPostponedVatCertificateFile(fileInformation)

        postponedVatCertificateFile must be(expectedPostponedVatCertificateFile)
      }

      "raise exception if mandatory information is missing" in  {
        val sdesGatekeeperService = new SdesGatekeeperService()

        val metadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("FileType", "PDF"),
          MetadataItem("FileRole", "PostponedVATStatement"),
          MetadataItem("DutyPaymentMethod", "Immediate")
        )
        metadata.foreach { item =>

          val incompleteMetadata = Metadata(metadata.filterNot(_ == item))

          assertThrows[NoSuchElementException] {

            val fileInformation = domain.FileInformation(
              "PostponedVATStatement_100000000323_pdf.pdf",
              "https://some.sdes.domain?token=abc123",
              1234L,
              incompleteMetadata
            )

            sdesGatekeeperService.convertToPostponedVatCertificateFile(fileInformation)
          }
        }
      }
    }

    "convertToSecurityStatementFile" should {

      "create SecurityStatementFile from FileInformation" in  {
        val sdesGatekeeperService = new SdesGatekeeperService()

        val securityStatementMetadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("PeriodStartDay", "1"),
          MetadataItem("PeriodEndYear", "2018"),
          MetadataItem("PeriodEndMonth", "6"),
          MetadataItem("PeriodEndDay", "8"),
          MetadataItem("FileType", "CSV"),
          MetadataItem("FileRole", "SecurityStatement"),
          MetadataItem("eoriNumber", "GB12345678"),
          MetadataItem("fileSize", "1234"),
          MetadataItem("checksum", "FF5")
        )

        val fileInformation = domain.FileInformation(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          Metadata(securityStatementMetadata)
        )

        val expectedSecurityStatementFile = SecurityStatementFile(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          SecurityStatementFileMetadata(2018, 6, 1, 2018, 6, 8, Csv, SecurityStatement, "GB12345678", 1234L, "FF5", None)
        )

        val securityStatementFile = sdesGatekeeperService.convertToSecurityStatementFile(fileInformation)

        securityStatementFile must be(expectedSecurityStatementFile)
      }

      "create SecurityStatementFile from FileInformation having requested statements id" in  {
        val sdesGatekeeperService = new SdesGatekeeperService()

        val requestedSecurityStatementMetadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("PeriodStartDay", "1"),
          MetadataItem("PeriodEndYear", "2018"),
          MetadataItem("PeriodEndMonth", "6"),
          MetadataItem("PeriodEndDay", "8"),
          MetadataItem("FileType", "CSV"),
          MetadataItem("FileRole", "SecurityStatement"),
          MetadataItem("eoriNumber", "GB12345678"),
          MetadataItem("fileSize", "1234"),
          MetadataItem("checksum", "FF5"),
          MetadataItem("statementRequestID", "id1234567")
        )

        val fileInformationForRequestedStatements = domain.FileInformation(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          Metadata(requestedSecurityStatementMetadata)
        )

        val expectedSecurityStatementFile = SecurityStatementFile(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          SecurityStatementFileMetadata(2018, 6, 1, 2018, 6, 8, Csv, SecurityStatement, "GB12345678", 1234L, "FF5", Some("id1234567"))
        )

        val securityStatementFile = sdesGatekeeperService.convertToSecurityStatementFile(fileInformationForRequestedStatements)

        securityStatementFile must be(expectedSecurityStatementFile)
      }
    }

    "convertTo[T]" should {

      "convert FileInformation to a given SDES File type as 'T'" in {
        val sdesGatekeeperService = new SdesGatekeeperService()

        val securityStatementMetadata = List(
          MetadataItem("PeriodStartYear", "2018"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("PeriodStartDay", "1"),
          MetadataItem("PeriodEndYear", "2018"),
          MetadataItem("PeriodEndMonth", "6"),
          MetadataItem("PeriodEndDay", "8"),
          MetadataItem("FileType", "CSV"),
          MetadataItem("FileRole", "SecurityStatement"),
          MetadataItem("eoriNumber", "GB12345678"),
          MetadataItem("fileSize", "1234"),
          MetadataItem("checksum", "FF5")
        )

        val fileInformation = domain.FileInformation(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          Metadata(securityStatementMetadata)
        )

        import sdesGatekeeperService._
        val expectedSecurityStatementFile = Seq(SecurityStatementFile(
          "securities-2018-06.csv",
          "https://some.sdes.domain?token=abc123",
          1234L,
          SecurityStatementFileMetadata(2018, 6, 1, 2018, 6, 8, Csv, SecurityStatement, "GB12345678", 1234L, "FF5", None)
        ))

        val securityStatementFile = convertTo[SecurityStatementFile]

        securityStatementFile(Seq(fileInformation)) must be(expectedSecurityStatementFile)
      }
    }

    "create StandingAuthorityFile from FileInformation" in  {
      val sdesGatekeeperService = new SdesGatekeeperService()

      val standingAuthorityFileMetadata = List(
        MetadataItem("PeriodStartYear", "2022"),
        MetadataItem("PeriodStartMonth", "6"),
        MetadataItem("PeriodStartDay", "1"),
        MetadataItem("FileType", "CSV"),
        MetadataItem("FileRole", "StandingAuthority")
      )

      val fileInformationForStandingAuthorityCSV = domain.FileInformation(
        "authorities-2022-06.csv",
        "https://some.sdes.domain?token=abc123",
        1234L,
        Metadata(standingAuthorityFileMetadata)
      )

      val expectedStandingAuthorityFile = StandingAuthorityFile(
        "authorities-2022-06.csv",
        "https://some.sdes.domain?token=abc123",
        1234L,
        StandingAuthorityMetadata(2022, 6, 1, Csv, StandingAuthority), ""
      )

      val standingAuthorityFile = sdesGatekeeperService.convertToStandingAuthoritiesFile(fileInformationForStandingAuthorityCSV)

      standingAuthorityFile must be(expectedStandingAuthorityFile)
    }
  }
}
//scalastyle:on magic.number
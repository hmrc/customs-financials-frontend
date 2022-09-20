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
import domain.FileRole.{PostponedVATStatement, StandingAuthority}
import domain.{Metadata, MetadataItem, PostponedVatStatementFile, PostponedVatStatementFileMetadata, StandingAuthorityFile, StandingAuthorityMetadata}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.SpecBase


//scalastyle:off magic.number
class SdesGatekeeperServiceSpec extends SpecBase {
  implicit val messages: Messages = Helpers.stubMessages()
  "SdesGatekeeperService" should {

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
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

package controllers

import domain.FileFormat.{Csv, Pdf}
import domain.FileRole.C79Certificate
import domain.{EoriHistory, VatCertificateFile, VatCertificateFileMetadata, VatCertificatesByMonth, VatCertificatesForEori}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.test.Helpers
import utils.SpecBase
import viewmodels.VatViewModel

import java.time.LocalDate

class VatViewModelSpec extends SpecBase   {
  implicit val messages: Messages = Helpers.stubMessages()
  "The VatViewModel" should {
      "indicate that there are no current statements" when {
      "none of the EORIs have any current statements" in {
        val vatStatementsForEori1 = VatCertificatesForEori(EoriHistory("GB12345", None, None), Seq.empty, Seq.empty)
        val vatStatementsForEori2 = VatCertificatesForEori(EoriHistory("GB67890", None, None), Seq.empty, Seq.empty)
        val vatModel = VatViewModel(Seq(vatStatementsForEori1, vatStatementsForEori2))
        vatModel.hasCurrentCertificates mustBe false
      }
    }

    "indicate that there are current statements" when {
      "some of the EORIs have any current statements" in {
        val vatCertificateFiles = List(
          VatCertificateFile("name_04", "download_url_06", 111L, VatCertificateFileMetadata(2018, 3, Pdf, C79Certificate, None), ""),
          VatCertificateFile("name_04", "download_url_05", 111L, VatCertificateFileMetadata(2018, 4, Csv, C79Certificate, None), ""))
        val currentStatements = Seq(VatCertificatesByMonth(LocalDate.now(), vatCertificateFiles))
        val vatStatementsForEori1 = VatCertificatesForEori(EoriHistory("GB12345", None, None), currentStatements, Seq.empty)
        val vatStatementsForEori2 = VatCertificatesForEori(EoriHistory("GB67890", None, None), Seq.empty, Seq.empty)
        val vatModel = VatViewModel(Seq(vatStatementsForEori1, vatStatementsForEori2))
        vatModel.hasCurrentCertificates mustBe true
      }
    }
  }

}

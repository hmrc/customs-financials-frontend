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

package views

import config.AppConfig
import domain.FileFormat.Pdf
import domain.{EoriHistory, FileRole, VatCertificateFile, VatCertificateFileMetadata, VatCertificatesByMonth, VatCertificatesForEori}
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.Helpers.running
import play.api.test.{FakeRequest, Helpers}
import utils.SpecBase
import viewmodels.VatViewModel
import views.html.import_vat.import_vat

import java.time.LocalDate

class ImportVatViewSpec extends SpecBase {

  "C79 certificates view" should {
    "have heading 'Import VAT certificates'" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("import-vat-certificates-heading")
      }
    }

    "display a list of current statements with download links" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("statements-list-0-row-0-date-cell")
        view(viewModelForHistoricEori).containsElementById("statements-list-0-row-0-pdf-download-link")
        view(viewModelForHistoricEori).containsElementById("statements-list-1-row-0-date-cell")
        view(viewModelForHistoricEori).containsElementById("statements-list-1-row-0-pdf-download-link")
      }
    }

    "display unavailable text when there are no statements" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("missing-file-statements-list-0-row-0-csv-download-link")
        view(viewModelForHistoricEori).containsElementById("missing-file-statements-list-1-row-0-csv-download-link")
      }
    }

    "display certificates section for historic EORIs" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("historic-eori-1")
        view(viewModelForHistoricEori).containsElementById("statements-list-1")
      }
    }

    "display the missing documents guidance" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("missing-documents-guidance-heading")
        view(viewModelForHistoricEori).containsElementById("missing-documents-guidance-text1")
        view(viewModelForHistoricEori).containsElementById("missing-documents-guidance-text2")
      }
    }

    "display message when there are no VAT certificates" in new Setup {
      running(app) {
        view(viewModelForHistoricEori.copy(Seq.empty)).containsElementById("no-certificates-available-text")
      }
    }

    "not display heading and section for historic EORIs that do not have certificates" in new Setup {
      running(app) {
        view(viewModelForNoHistoricEori).notContainElementById("historic-eori-1")
        view(viewModelForNoHistoricEori).notContainElementById("statements-list-1")
      }
    }

    "displays link to historic statements request" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("historic-statement-request")
      }
    }

    "display a help and support message" in new Setup {
      running(app) {
        view(viewModelForHistoricEori).containsElementById("help_and_support")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val messages = Helpers.stubMessages()
    implicit val request = FakeRequest("GET", "/some/resource/path")

    val vatCertificateFiles = List(
      VatCertificateFile("file1", "url-download-1", 123L, VatCertificateFileMetadata(2018, 6, Pdf, FileRole.C79Certificate, None), ""),
      VatCertificateFile("file2", "url-download-2", 124L, VatCertificateFileMetadata(2018, 7, Pdf, FileRole.C79Certificate, None), ""))

    val currentDate = LocalDate.of(2020, 3, 1)
    val currentCertificates = Seq(VatCertificatesByMonth(currentDate, vatCertificateFiles))
    val certificatesForCurrentEori = VatCertificatesForEori(EoriHistory("current-eori", None, None), currentCertificates, Seq.empty)
    val certificatesForHistoricEori = VatCertificatesForEori(EoriHistory("historic-eori", None, None), currentCertificates, Seq.empty)
    val viewModelForHistoricEori = VatViewModel(Seq(certificatesForCurrentEori, certificatesForHistoricEori))
    val noCertificatesForHistoricEori = VatCertificatesForEori(EoriHistory("historic-eori", None, None), Seq.empty, Seq.empty)
    val viewModelForNoHistoricEori = VatViewModel(Seq(certificatesForCurrentEori, noCertificatesForHistoricEori))

    val app = application().build()

    implicit val appConfig = app.injector.instanceOf[AppConfig]
    def view(viewModel: VatViewModel) = Jsoup.parse(app.injector.instanceOf[import_vat].apply(viewModel).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

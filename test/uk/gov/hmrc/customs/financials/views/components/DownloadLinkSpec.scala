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

package uk.gov.hmrc.customs.financials.views.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import play.api.test.Helpers
domain.FileFormat.Pdf
domain.FileRole.C79Certificate
domain.{VatCertificateFile, VatCertificateFileMetadata}
utils.SpecBase
views

class DownloadLinkSpec extends SpecBase {

  "Download link component" should {

    "display the file type, size and units" in {
      implicit val messages = Helpers.stubMessages()

      val file = VatCertificateFile("name_04", "download_url_06", 111L, VatCertificateFileMetadata(2018, 3, Pdf, C79Certificate, None), "")

      val content: Element = Jsoup.parse(views.html.components.download_link(Some(file), Pdf, "id", "period").body)
      content.getElementById("id").text() mustBe "PDF (1KB)"
    }
  }
}

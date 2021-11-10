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

package uk.gov.hmrc.customs.financials.views

import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
config.AppConfig
utils.SpecBase
views.html.import_vat_not_available

class ImportVatNotAvailableViewSpec extends SpecBase {

  "Certificates not available view" should {
    "contain a heading" in new Setup {
      running(app) {
        view.getElementById("import-vat-certificates-heading").text mustBe "Import VAT certificates (C79)"
      }
    }

    "contain a message section" in new Setup {
      running(app) {
        view.getElementById("no-certificates-available-text").text mustBe "Sorry, your certificates are not available at the moment. Please try again later."
      }
    }

    "contain a missing documents section" in new Setup {
      running(app) {
        view.getElementById("missing-documents-guidance").text mustBe "Can’t find the certificate you’re looking for? Certificates for import declarations made in CHIEF are not available in this service. Certificates are only generated for periods in which you imported goods."
      }
    }

    "contain a historic certificate request section" in new Setup {
      running(app) {
        view.getElementById("historic-certificate-request").text mustBe "We only show certificates for the last 6 months. If required, you can request older certificates."
      }
    }

    "contain a help and support section" in new Setup {
      running(app) {
        view.getElementById("help_and_support").text mustBe "Help and support If you are having issues, phone 0300 200 3701. Open 8am to 6pm, Monday to Friday (closed bank holidays)."
      }
    }

  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")

    val app = application().build()

    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val view = Jsoup.parse(app.injector.instanceOf[import_vat_not_available].apply().body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

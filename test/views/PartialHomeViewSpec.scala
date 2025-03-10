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

package views

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase
import views.html.dashboard.customs_financials_partial_home
import utils.MustMatchers

class PartialHomeViewSpec extends SpecBase with MustMatchers {

  "Customs Financials Partial Home View" should {
    "display header as a link text" in new Setup {
      running(app) {
        view.getElementsByClass("govuk-header__link").text mustBe "GOV.UK Manage import duties and VAT accounts"
      }
    }

    "display a heading" in new Setup {
      running(app) {
        view
          .getElementsByClass("govuk-heading-xl")
          .text mustBe "Sorry, some parts of the service are unavailable at the moment"
      }
    }

    "not display other accounts you can use link" in new Setup {
      running(app) {
        view.notContainElementById("authority-to-use-link")
      }
    }

    "not display manage your account authorities link" in new Setup {
      running(app) {
        view.notContainElementById("manage-account-authorities-link")
      }
    }

    "display the notification panel" in new Setup {
      running(app) {
        view.containsElementById("notification-panel")
      }
    }

    "display the statement cards" in new Setup {
      running(app) {
        view.containsElementById("import-vat")
        view.containsElementById("postponed-vat")
        view.containsElementById("import-adjustments")
      }
    }
  }

  trait Setup extends I18nSupport {
    val eori: String                   = "EORI0123"
    val notificationsKeys: Seq[String] = Seq("c79")

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application                                      = application().build()
    implicit val appConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

    def view: Document =
      Jsoup.parse(app.injector.instanceOf[customs_financials_partial_home].apply(eori, notificationsKeys).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

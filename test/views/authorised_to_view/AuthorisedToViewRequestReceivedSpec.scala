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

package views.authorised_to_view

import config.AppConfig
import domain.{AccountLinkWithoutDate, CompanyAddress}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase
import views.html.authorised_to_view.authorised_to_view_request_received
import utils.MustMatchers

class AuthorisedToViewRequestReceivedSpec extends SpecBase with MustMatchers {

  "Customs Financials Authorised to View request received" should {

    "display by h tags" should {
      "display header" in new Setup {
        running(app) {
          view.getElementsByTag("h1").text mustBe
            msg("cf.authorities.request.received.panel.h1")
        }
      }

      "display help link" in new Setup {
        running(app) {
          view.getElementsByTag("h2").text mustBe
            "Help make GOV.UK better What happens next Support links"
        }
      }
    }

    "display by Id" should {
      "display header" in new Setup {
        running(app) {
          view.getElementById("cf.authorities.request.received.panel.h1").text mustBe
            msg("cf.authorities.request.received.panel.h1")
        }
      }

      "display label" in new Setup {
        running(app) {
          view.getElementById("cf.authorities.label").text mustBe msg("cf.authorities.label")
        }
      }

      "display next" in new Setup {
        running(app) {
          view.getElementById("cf.authorities.next").text mustBe msg("cf.authorities.next")
        }
      }

      "display next msg" in new Setup {
        running(app) {
          view.getElementById("cf.authorities.next.msg").text mustBe msg("cf.authorities.next.msg")
        }
      }

      "display link with correct href" in new Setup {
        running(app) {
          val linkElement = view.getElementById("cf.authorities.request.received.link")
            .getElementsByTag("a")
            .first()

          linkElement.text mustBe msg("cf.authorities.request.received.link")
          linkElement.attr("href") mustBe expectedUrl
        }
      }
    }
  }

  trait Setup {
    val eori: String = "EORI0123"
    val email = "email@emailland.com"

    val accountLink: AccountLinkWithoutDate = new AccountLinkWithoutDate(
      eori, false, "123", "1", Some(1), "2345678")

    val accountNumbers: Seq[AccountLinkWithoutDate] = Seq(accountLink, accountLink)

    val companyAddress: CompanyAddress = new CompanyAddress(
      streetAndNumber = "123Street",
      city = "city",
      postalCode = Some("postcode"),
      countryCode = "CountryCode")

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val expectedUrl: String = appConfig.manageAuthoritiesFrontendUrl

    def view: Document = Jsoup.parse(
      app.injector.instanceOf[authorised_to_view_request_received].apply(email).body)

    implicit val msg: Messages = messages(app)
  }
}

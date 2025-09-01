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

package views.your_contact_details

import config.AppConfig
import domain.{AccountLinkWithoutDate, CompanyAddress}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.{MustMatchers, SpecBase}
import views.html.your_contact_details.your_contact_details
import play.twirl.api.HtmlFormat
import utils.TestData.TEST_MESSAGE_BANNER

class YourContactDetailsViewSpec extends SpecBase with MustMatchers {

  "Customs Financials Your Contact Details" should {
    "display header" in new Setup {
      running(app) {
        view().getElementsByTag("h1").text mustBe "Your contact details"
      }
    }

    "display second header text" in new Setup {
      running(app) {
        view().getElementsByTag("h2").text mustBe "Help make GOV.UK better " +
          "Company details Primary email address Duty deferment contact details Support links"
      }
    }

    "display the govlink" in new Setup {
      running(app) {
        view().getElementsByClass("govuk-link")
      }
    }

    "display link to view or change" in new Setup {
      running(app) {
        view().containsLinkWithText("#", "View or change")
      }
    }

    "display link to report a change" in new Setup {
      running(app) {
        view().containsLinkWithText(
          appConfig.reportChangeCdsUrl,
          "Report a change to your company details (opens in new tab)"
        )
      }
    }

    "display link to get enquiry form for your contact details" in new Setup {
      running(app) {
        view().getElementById("eori-change-or-cancel-link").text() mustBe
          "This is the contact address you gave us when you registered for your EORI number." +
          " You can fill in a change of circumstance form (opens in a new tab) to change this address."
      }
    }

    "display the message banner partial when provided" in new Setup {
      val pageView: Document        = view(Some(HtmlFormat.fill(Seq(TEST_MESSAGE_BANNER))))
      val bannerComponent: Elements = pageView.getElementsByClass("notifications-bar")

      bannerComponent.size() must be > 0

      assert(pageView.containsLinkWithText("http://localhost:9876/customs/payment-records", "Home"))
      assert(pageView.containsLink("http://localhost:9842/customs/secure-messaging/inbox?return_to=test_url"))

      assert(
        pageView.containsLinkWithText(
          "http://localhost:9876/customs/payment-records/your-contact-details",
          "Your contact details"
        )
      )
      assert(
        pageView.containsLinkWithText("http://localhost:9000/customs/manage-authorities", "Your account authorities")
      )
    }
  }

  trait Setup extends I18nSupport {
    val eori: String              = "EORI0123"
    val email                     = "email@emailland.com"
    val companyName: Some[String] = Some("CompanyName")

    val accountLink: AccountLinkWithoutDate = new AccountLinkWithoutDate(eori, false, "123", "1", Some(1), "2345678")

    val accountNumbers: Seq[AccountLinkWithoutDate] = Seq(accountLink, accountLink)

    val companyAddress: CompanyAddress = new CompanyAddress(
      streetAndNumber = "123Street",
      city = "city",
      postalCode = Some("postcode"),
      countryCode = "CountryCode"
    )

    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application                                      = application().build()
    implicit val appConfig: AppConfig                         = app.injector.instanceOf[AppConfig]

    def view(messageBannerPartial: Option[HtmlFormat.Appendable] = None): Document =
      Jsoup.parse(
        app.injector
          .instanceOf[your_contact_details]
          .apply(eori, accountNumbers, companyName, companyAddress, email, messageBannerPartial)
          .body
      )

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

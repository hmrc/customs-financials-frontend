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
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.views.html.guarantee_account_not_available

class GuaranteeAccountNotAvailableViewSpec extends SpecBase {

  "GuaranteeAccountNotAvailableView" should {
    "page heading" in new Setup {
      running(app) {
        view.getElementsByClass("govuk-heading-xl").text mustBe "General guarantee account"
      }
    }

    "display guarantee account transactions not available message" in new Setup {
      running(app) {
        view.getElementById("account-not-available").text mustBe "We are unable to show your account at the moment. Please try again later."
      }
    }

    "display help & support message" in new Setup {
      running(app) {
        view.getElementById("help_and_support").text mustBe "Help and support If you are having issues, phone 0300 200 3701. Open 8am to 6pm, Monday to Friday (closed bank holidays)."
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val view = Jsoup.parse(app.injector.instanceOf[guarantee_account_not_available].apply().body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

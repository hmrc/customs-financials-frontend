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
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import utils.SpecBase
import views.html.error_states.not_subscribed_to_cds

class NotSubscribedToCdsViewSpec extends SpecBase {

  "Not subscribed to cds view" should {
    "display header as non-link text" in new Setup{
      running(app) {
        view.getElementsByClass("govuk-header__link--service-name").text mustBe "View your customs financial accounts"
      }
    }

    "display page heading" in new Setup{
      running(app) {
        view.getElementsByTag("h1").text mustBe "To continue with this you need to get access to Customs Declaration Services (CDS)"
      }
    }

    "display get access to cds service links" in new Setup{
      running(app) {
        view.containsLinkWithText("/customs/register-for-cds", "Economic Operator and Registration Identification (EORI) number (opens in a new window or tab)")
        view.containsLinkWithText(appConfig.subscribeCdsUrl, "get access to CDS (opens in a new window or tab)")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val view = Jsoup.parse(app.injector.instanceOf[not_subscribed_to_cds].apply().body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

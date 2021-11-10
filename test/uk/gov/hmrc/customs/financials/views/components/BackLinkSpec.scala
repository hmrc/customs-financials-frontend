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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
config.AppConfig
utils.SpecBase
views.html.components.back_link

class BackLinkSpec extends SpecBase  {

  "back link component" should {
    "render correct link" in new Setup {
      running(app) {
        content.getElementsByTag("a").attr("href") mustBe url
      }
    }

    "render Back label" in new Setup {
      running(app) {
        content.getElementsByTag("a").text mustBe "Back"
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val url = "/sample/link"
    val content = Jsoup.parse(app.injector.instanceOf[back_link].apply(url).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }

}

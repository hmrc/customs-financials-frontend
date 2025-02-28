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
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.SpecBase
import views.html.error_states.not_subscribed_to_cds
import utils.MustMatchers

class NotSubscribedToCdsViewSpec extends SpecBase with MustMatchers {

  "NotSubscribedToCds view" should {

    "display the page heading" in new Setup {
      view.getElementsByTag("h1").text mustBe messages("cf.not-subscribed-to-cds.detail.heading")
    }

    "contain correct links for getting access to CDS" in new Setup {
      val subscribeLink = view.select(s"a#link-item-link-1")
      subscribeLink mustNot be(empty)
      subscribeLink.attr("href") mustBe appConfig.subscribeCdsUrl
      subscribeLink.text must include(messages("cf.not-subscribed-to-cds.detail.list-item.1.link"))

      val eoriLink = view.select(s"a#link-item-link-3")
      eoriLink mustNot be(empty)
      eoriLink.attr("href") mustBe appConfig.manageTeamMembersUrl
      eoriLink.text must include(messages("cf.not-subscribed-to-cds.detail.list-item.3.link"))
    }

    "include a service help link" in new Setup {
      val helpLink = view.select(s"a#service-help-link")
      helpLink mustNot be(empty)
      helpLink.attr("href") mustBe appConfig.onlineServicesHelpUrl
      helpLink.text must include(messages("cf.not-subscribed-to-cds.detail.service-help.link"))
    }
  }

  trait Setup extends I18nSupport {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    implicit val messages: Messages = messagesApi.preferred(request)
    val view: Document = Jsoup.parse(app.injector.instanceOf[not_subscribed_to_cds].apply().body)
  }
}

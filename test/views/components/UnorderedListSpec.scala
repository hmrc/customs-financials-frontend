/*
 * Copyright 2025 HM Revenue & Customs
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

package views.components

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.SpecBase
import views.html.components.unordered_list
import utils.MustMatchers
import play.twirl.api.Html

class UnorderedListSpec extends SpecBase with MustMatchers {

  "UnorderedList component" should {

    "render a bulleted list with the correct structure" in new Setup {
      listView.select("ul.govuk-list.govuk-list--bullet") mustNot be(empty)
    }

    "contain the correct number of list items" in new Setup {
      val listItems = listView.select("ul.govuk-list--bullet li")
      listItems.size() mustBe 3
    }

    "display the expected list items in order" in new Setup {
      val listItems = listView.select("ul.govuk-list--bullet li").eachText()
      listItems must contain inOrderOnly(
        messages("cf.not-subscribed-to-cds.detail.list-item.1"),
        messages("cf.not-subscribed-to-cds.detail.list-item.2"),
        messages("cf.not-subscribed-to-cds.detail.list-item.3")
      )
    }
  }

  trait Setup extends I18nSupport {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app                                                   = application().build()
    implicit val messagesApi: MessagesApi                     = app.injector.instanceOf[MessagesApi]
    implicit val messages: Messages                           = messagesApi.preferred(request)
    val unorderedListComponent: unordered_list                = app.injector.instanceOf[unordered_list]

    val listHtml: Html = unorderedListComponent.apply(
      Html(messages("cf.not-subscribed-to-cds.detail.list-item.1")),
      Html(messages("cf.not-subscribed-to-cds.detail.list-item.2")),
      Html(messages("cf.not-subscribed-to-cds.detail.list-item.3"))
    )

    val listView: Document = Jsoup.parse(listHtml.body)
  }
}

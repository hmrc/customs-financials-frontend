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
import play.api.test.Helpers.running
import utils.SpecBase
import views.html.error_states.not_found_template
import org.scalatest.matchers.must.{Matchers => MustMatchers}

import scala.jdk.CollectionConverters._

class NotFoundTemplateSpec extends SpecBase with MustMatchers {

  "Not found template view" should {

    "display a page heading" in new Setup {
      running(app) {
        view.getElementsByTag("h1").text mustBe "Page not found"
      }
    }

    "display body text" in new Setup {
      running(app) {
        val expected = List(
          "If you typed the web address, check it is correct.",
          "If you pasted the web address, check you copied the entire address.",
          "You can go back to manage import duties and VAT accounts."
        )
        val actual =
          view.getElementsByClass("govuk-body").asScala.map(_.text()).toList
        actual mustBe expected
      }
    }

    "display link to landing page" in new Setup {
      running(app) {
        view.containsLink(
          controllers.routes.CustomsFinancialsHomeController.index.url
        ) mustBe true
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val view: Document = Jsoup.parse(app.injector.instanceOf[not_found_template].apply().body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

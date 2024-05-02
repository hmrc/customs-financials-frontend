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

package views.helpers

import utils.SpecBase
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder

class PageTitleSpec extends SpecBase {

  private val messagesApi: MessagesApi = new GuiceApplicationBuilder().build().injector.instanceOf[MessagesApi]

  "PageTitle helper object" should {

    "generate full page title with provided title" in {
      val messages: Messages = messagesApi.preferred(Seq.empty)
      val title = Some("Page Title")
      val fullTitle = PageTitle.fullPageTitle(title)(messages)
      fullTitle shouldBe Some("Page Title - Manage import duties and VAT accounts - GOV.UK")
    }

    "generate full page title without provided title" in {
      val messages: Messages = messagesApi.preferred(Seq.empty)
      val title = None
      val fullTitle = PageTitle.fullPageTitle(title)(messages)

      fullTitle shouldBe Some("Manage import duties and VAT accounts - GOV.UK")
    }
  }
}

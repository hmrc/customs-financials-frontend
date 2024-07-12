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

package views.components

import domain.{AccountStatusClosed, AccountStatusOpen, AccountStatusPending, AccountStatusSuspended}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.SpecBase
import views.html.components.account_status

class AccountStatusSpec extends SpecBase {

  "render correct status message for pending status" in new Setup {
    val output: HtmlFormat.Appendable = view(
      status = AccountStatusPending,
      accountType = "duty-deferment"
    )(messages(app))

    val html: Document = Jsoup.parse(contentAsString(output))

    html.getElementsByClass("duty-deferment-status").text() must include("Pending Opening On CDS")
    Option(html.getElementsByClass("account-status-header")) must not be empty
  }

  "render correct status message for closed status" in new Setup {
    val output: HtmlFormat.Appendable = view(
      status = AccountStatusClosed,
      accountType = "duty-deferment"
    )(messages(app))

    val html: Document = Jsoup.parse(contentAsString(output))

    html.getElementsByClass("duty-deferment-status").text() must include("Closed")
    Option(html.getElementsByClass("account-status-header")) must not be empty
  }

  "render correct status message for suspended status" in new Setup {
    val output: HtmlFormat.Appendable = view(
      status = AccountStatusSuspended,
      accountType = "duty-deferment"
    )(messages(app))

    val html: Document = Jsoup.parse(contentAsString(output))

    html.getElementsByClass("duty-deferment-status").text() must include("Action Required")
    Option(html.getElementsByClass("account-status-header")) must not be empty
  }

  "not render status message for open duty-deferment status" in new Setup {
    val output: HtmlFormat.Appendable = view(
      status = AccountStatusOpen,
      accountType = "duty-deferment"
    )(messages(app))

    val html: Document = Jsoup.parse(contentAsString(output))

    html.getElementsByTag("body").text() mustNot include("Open Status Message")
    Option(html.getElementsByClass("account-status-header")) must not be empty
  }

  trait Setup {
    val app: Application = application().build()
    val view: account_status = app.injector.instanceOf[account_status]
  }
}

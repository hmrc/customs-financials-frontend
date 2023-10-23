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

import play.api.Application
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.twirl.api.{HtmlFormat}
import utils.SpecBase
import views.html.components.p

class PSpec extends SpecBase {
    "P component" should {
        "render the default class name when classes is not defined" in new SetUp {
            running(app) {
                val pView = app.injector.instanceOf[p]
                val output: HtmlFormat.Appendable = pView(
                    message = "Hello, world!",
                )(messages(app))
                val html: Document = Jsoup.parse(contentAsString(output))

                html.getElementsByTag("p").attr("class") must include("govuk-body")
            }
        }

        "render the message and classes correctly" in new SetUp {
            running(app) {
                val pView = app.injector.instanceOf[p]
                val output: HtmlFormat.Appendable = pView(
                    message = "Hello, world!",
                    extraClasses = "custom-class"
                )(messages(app))
                val html: Document = Jsoup.parse(contentAsString(output))

                html.getElementsByClass("custom-class").text() must include("Hello, world!")
            }
        }

    }

    trait SetUp {
        val app: Application = application().build()
    }
}

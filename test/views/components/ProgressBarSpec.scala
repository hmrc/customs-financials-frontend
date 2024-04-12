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
import play.twirl.api.HtmlFormat
import utils.SpecBase
import views.html.components.progress_bar

class ProgressBarSpec extends SpecBase {

  "ProgressBar" should {

    "should generate correct HTML" in {
      val usedPercentage = 90
      val app: Application = application().build()

      running(app) {
        val view = app.injector.instanceOf[progress_bar]
        val output: HtmlFormat.Appendable = view(usedPercentage)
        val html: Document = Jsoup.parse(contentAsString(output))

        html.getElementsByTag("span").attr("class") must include("graph-90")
      }
    }
  }
}

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
import play.api.test.Helpers
import play.api.test.Helpers.running
config.AppConfig
utils.SpecBase
views

class RecruitmentBannerSpec extends SpecBase {

  "Recruitment banner" should {
    "have a title" in new Setup {
      running(app) {
        val page = Jsoup.parse(views.html.components.recruitment_banner()(messages, appConfig).body)

        page.getElementsByTag("h2").isEmpty mustBe false
      }
    }

    "have a link to an external site" in new Setup {
      running(app) {
        val page = Jsoup.parse(views.html.components.recruitment_banner()(messages, appConfig).body)
        page.containsLink("https://signup.take-part-in-research.service.gov.uk?utm_campaign=CDSfinancials&utm_source=Other&utm_medium=other&t=HMRC&id=249")
      }

    }

    "have link to close banner" in new Setup {
      running(app) {
        val page = Jsoup.parse(views.html.components.recruitment_banner()(messages, appConfig).body)

        page.getElementsByTag("a").attr("href") mustBe "#"
      }
    }

    "include the hidden text for close banner" in new Setup {
      running(app) {
        val page = Jsoup.parse(views.html.components.recruitment_banner()(messages, appConfig).body)

        page.getElementsByTag("span").first().getElementsByClass("govuk-visually-hidden").text mustBe
          "cf.customs-financials-home.recruitment-banner.hidden-text.close"
      }
    }
  }

  trait Setup {
    val app = application().build()
    implicit val messages = Helpers.stubMessages()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
  }
}

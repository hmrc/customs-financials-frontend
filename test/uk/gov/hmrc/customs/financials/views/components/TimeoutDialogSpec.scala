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
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.views

class TimeoutDialogSpec extends SpecBase  {

  "TimeoutDialog" should {
    val requestedUri = "/customs/payment-records"
    val timeout = "900"
    val content = Jsoup.parse(views.html.components.timeout_dialog(requestedUri, timeout).body)

    "include data attributes required for TimeoutDialog.js modal to render" in {
      content.getElementsByAttribute("data-timeout").isEmpty mustBe false
      content.getElementsByAttribute("data-countdown").isEmpty mustBe false
      content.getElementsByAttribute("data-keep-alive-url").isEmpty mustBe false
      content.getElementsByAttribute("data-sign-out-url").isEmpty mustBe false
    }
  }

}

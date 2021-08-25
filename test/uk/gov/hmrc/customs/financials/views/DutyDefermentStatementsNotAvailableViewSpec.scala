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

package uk.gov.hmrc.customs.financials.views

import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.views.html.duty_deferment_statements_not_available

class DutyDefermentStatementsNotAvailableViewSpec extends SpecBase {

  "Statements not available view" should {
    "contain an account number heading" in new Setup {
      running(app) {
        view.containsElementById("account-number-heading")
      }
    }

    "contain a statements heading" in new Setup {
      running(app) {
        view.containsElementById("statements-heading")
      }
    }

    "contain a message section" in new Setup {
      running(app) {
        view.containsElementById("no-statements")
      }
    }

    "contain a missing documents section" in new Setup {
      running(app) {
        view.containsElementById("missing-documents-guidance")
      }
    }

    "contain a historic statement request section" in new Setup {
      running(app) {
        view.containsElementById("historic-statement-request")
      }
    }

    "contain a help and support section" in new Setup {
      running(app) {
        view.containsElementById("help_and_support")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()

    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val view = Jsoup.parse(app.injector.instanceOf[duty_deferment_statements_not_available].apply("accNumber", "linkId").body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

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
import play.api.i18n.Messages
import utils.SpecBase
import views.html.components.authorised_account_search_company_name

class AuthorisedAccountSearchCompanyName extends SpecBase {
  "AuthorisedAccountSearchCompanyName view" should {
    "load correctly and display correct guidance" in new Setup {

      val view = app.injector.instanceOf[authorised_account_search_company_name].apply(
        Option("TestCompnay"),
        "GBN45365789211",
        displayLink = true)


    }

    "display only GB EORI when only GB EORI is present" in new Setup {

    }

    "display only XI EORI when only XI EORI is present" in new Setup {

    }

    "display both GB and XI EORI when both are present" in new Setup {

    }
  }

  trait Setup {
    val app: Application = application().build()
    //implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    //implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
  }
}

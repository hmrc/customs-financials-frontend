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

package config

import play.api.Application
import utils.SpecBase

class AppConfigSpec extends SpecBase {

  "AppConfig" should {
    "contain correct values for the provided configuration" in new Setup {
      appConfig.appName shouldBe "customs-financials-frontend"
      appConfig.loginUrl shouldBe "http://localhost:9553/bas-gateway/sign-in"
      appConfig.loginContinueUrl shouldBe "http://localhost:9876/customs/payment-records"
      appConfig.signOutUrl shouldBe "http://localhost:9553/bas-gateway/sign-out-without-state"

      appConfig.cashAccountTopUpGuidanceUrl shouldBe
        "https://www.gov.uk/guidance/paying-into-your-cash-account-for-cds-declarations"

      appConfig.cashAccountWithdrawUrl shouldBe 
        "https://www.gov.uk/guidance/withdraw-funds-from-your-cash-account-for-cds-declarations"

      appConfig.feedbackService shouldBe 
        "https://www.development.tax.service.gov.uk/feedback/CDS-FIN"
      appConfig.timeout shouldBe 900
      appConfig.countdown shouldBe 120
    
      appConfig.helpMakeGovUkBetterUrl shouldBe
        "https://signup.take-part-in-research.service.gov.uk?" +
          "utm_campaign=CDSfinancials&utm_source=Other&utm_medium=other&t=HMRC&id=249"

      appConfig.subscribeCdsUrl shouldBe
        "https://www.tax.service.gov.uk/customs-enrolment-services/cds/subscribe"
    }
  }
  trait Setup {
    val app: Application = application().build()
    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  }
}

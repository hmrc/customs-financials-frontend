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

package domain

import utils.SpecBase

class DutyDefermentDisplayBalanceSpec extends SpecBase {

  "DutyDefermentDisplayBalance" should {

    "correctly handle None values for accountLimit, guaranteeLimit, and guaranteeLimitRemaining" in {
      val balance = DutyDefermentDisplayBalance(None, None, None)
      
      balance.accountLimit shouldBe None
      balance.guaranteeLimit shouldBe None
      balance.guaranteeLimitRemaining shouldBe None
    }

    "correctly handle Some values for accountLimit, guaranteeLimit, and guaranteeLimitRemaining" in {
      val accountLimit = Some("1000.00")
      val guaranteeLimit = Some("500.00")
      val guaranteeLimitRemaining = Some("200.00")

      val balance = DutyDefermentDisplayBalance(accountLimit, guaranteeLimit, guaranteeLimitRemaining)
      
      balance.accountLimit shouldBe accountLimit
      balance.guaranteeLimit shouldBe guaranteeLimit
      balance.guaranteeLimitRemaining shouldBe guaranteeLimitRemaining
    }
  }
}

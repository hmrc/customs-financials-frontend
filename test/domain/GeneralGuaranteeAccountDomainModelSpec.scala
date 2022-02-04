/*
 * Copyright 2022 HM Revenue & Customs
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
import domain.{GeneralGuaranteeAccount => domainGGA, GeneralGuaranteeAccountResponse => onwireGGA}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

class GeneralGuaranteeAccountDomainModelSpec extends SpecBase    {

  "General Guarantee Account Domain Model" should {

    "correctly generate a domain model when a account, limit and balance are available" in {

      val account = AccountResponse("number", "type", "owner", Some(AccountStatusClosed), Some(AccountCancelled), true, None)
      val generalGuaranteeAccount  = onwireGGA(account, guaranteeLimit = Some("1"), availableGuaranteeBalance = Some("2"))
      val expectedGGA = domainGGA("number", "owner", AccountStatusClosed, AccountCancelled, Some(GeneralGuaranteeBalance(BigDecimal(1), BigDecimal(2))))

      generalGuaranteeAccount.toDomain() must be (expectedGGA)
    }

    "correctly generate a domain model when limit and balance are not available" in {

      val account = AccountResponse("number", "type", "owner", Some(AccountStatusClosed), Some(AccountCancelled), false, None)
      val generalGuaranteeAccount  = onwireGGA(account, guaranteeLimit = None, availableGuaranteeBalance = None)
      val expectedGGA = domainGGA("number", "owner", AccountStatusClosed, AccountCancelled, None)

      generalGuaranteeAccount.toDomain() must be (expectedGGA)
    }

  }
}

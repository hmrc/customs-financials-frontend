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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsString, JsSuccess, Json}
import utils.SpecBase

class CDSAccountStatusSpec extends SpecBase {

  "CDSAccountStatusReads" should {
    "read correctly" in {

      import CDSAccountStatus.CDSAccountStatusReads

      Json.fromJson(JsString("Open")) mustBe JsSuccess(AccountStatusOpen)
      Json.fromJson(JsString("Suspended")) mustBe JsSuccess(AccountStatusSuspended)
      Json.fromJson(JsString("Closed")) mustBe JsSuccess(AccountStatusClosed)
      Json.fromJson(JsString("Pending")) mustBe JsSuccess(AccountStatusPending)

      Json.fromJson(JsString("open")) mustBe JsSuccess(AccountStatusOpen)
      Json.fromJson(JsString("suspended")) mustBe JsSuccess(AccountStatusSuspended)
      Json.fromJson(JsString("closed")) mustBe JsSuccess(AccountStatusClosed)
      Json.fromJson(JsString("pending")) mustBe JsSuccess(AccountStatusPending)

      Json.fromJson(JsString("Unknown")) mustBe JsSuccess(AccountStatusOpen)
      Json.fromJson(JsString("unknown")) mustBe JsSuccess(AccountStatusOpen)
    }

    "write correctly" in {

      import CDSAccountStatus.CDSAccountStatusReads

      Json.toJson[CDSAccountStatus](AccountStatusOpen) mustBe JsString("open")
      Json.toJson[CDSAccountStatus](AccountStatusSuspended) mustBe JsString("suspended")
      Json.toJson[CDSAccountStatus](AccountStatusClosed) mustBe JsString("closed")
      Json.toJson[CDSAccountStatus](AccountStatusPending) mustBe JsString("pending")
    }
  }
}

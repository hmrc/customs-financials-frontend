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

package uk.gov.hmrc.customs.financials.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._
utils.SpecBase

class PathBindableSpec extends SpecBase with Matchers {

  "optionBindable" should {

    "bind a path param to optional string" in {
      optionBindable.bind("key", "value").right.map {
        result => result must be (Some("value"))
      }
    }

    "unbind an optional string value to path param" in {
      val result = optionBindable.unbind("key", Some("value"))
      result must be ("value")
    }
  }
}

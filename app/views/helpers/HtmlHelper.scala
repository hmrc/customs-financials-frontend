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

package views.helpers

import domain.{AccountStatusClosed, AccountStatusOpen, AccountStatusPending, AccountStatusSuspended, CDSAccountStatus}

object HtmlHelper {

  implicit class Attribute(val status: CDSAccountStatus) {

    val statusAttribute: String = status match {
      case AccountStatusOpen => "govuk-tag"
      case AccountStatusSuspended => "govuk-tag govuk-tag--yellow"
      case AccountStatusClosed => "govuk-tag govuk-tag--grey"
      case AccountStatusPending => "govuk-tag govuk-tag--blue"
    }

    val isOpen: Boolean = status == AccountStatusOpen
    val isSuspended: Boolean = status == AccountStatusSuspended
    val isPending: Boolean = status == AccountStatusPending
  }

}

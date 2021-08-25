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

package uk.gov.hmrc.customs.financials.views.helpers

import uk.gov.hmrc.customs.financials.domain.{AccountStatusClosed, AccountStatusOpen, AccountStatusPending, AccountStatusSuspended, CDSAccountStatus}

object HtmlHelper {

  implicit class Attribute(val status: CDSAccountStatus) {

    val statusAttribute: String = status match {
      case AccountStatusOpen => "account-status-open"
      case AccountStatusSuspended => "account-status-suspended"
      case AccountStatusClosed => "account-status-closed"
      case AccountStatusPending => "account-status-pending"
    }

    val isOpen: Boolean = status == AccountStatusOpen
    val isSuspended: Boolean = status == AccountStatusSuspended
    val isPending: Boolean = status == AccountStatusPending

    val balanceAttribute: String = status match {
      case AccountStatusOpen => "account-balance-status-open"
      case AccountStatusSuspended => "account-balance-status-suspended"
      case AccountStatusPending => "account-balance-status-pending"
      case AccountStatusClosed => "account-balance-status-closed"
    }
  }

}

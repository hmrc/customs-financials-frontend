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

import java.time.LocalDateTime

case class AccountLink(
  sessionId: String,
  eori: EORI,
  isNiAccount: Boolean,
  accountNumber: String,
  accountStatus: CDSAccountStatus,
  accountStatusId: Option[CDSAccountStatusId],
  linkId: String,
  lastUpdated: LocalDateTime
) {

  def this(id: String, sessionCacheAccountLink: SessionCacheAccountLink) =
    this(
      id,
      sessionCacheAccountLink.eori,
      sessionCacheAccountLink.isNiAccount,
      sessionCacheAccountLink.accountNumber,
      sessionCacheAccountLink.accountStatus,
      sessionCacheAccountLink.accountStatusId,
      sessionCacheAccountLink.linkId,
      LocalDateTime.now()
    )
}

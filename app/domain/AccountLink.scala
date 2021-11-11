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

package domain

import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat, Reads, Writes}

case class AccountLink(sessionId: String,
                       eori: EORI,
                       accountNumber: String,
                       accountStatus: CDSAccountStatus,
                       accountStatusId: Option[CDSAccountStatusId],
                       linkId: String,
                       lastUpdated: DateTime
                      ){

  def this(id: String, sessionCacheAccountLink: SessionCacheAccountLink) = {
    this(id,
      sessionCacheAccountLink.eori,
      sessionCacheAccountLink.accountNumber,
      sessionCacheAccountLink.accountStatus,
      sessionCacheAccountLink.accountStatusId,
      sessionCacheAccountLink.linkId,
      DateTime.now()
    )
  }
}

object AccountLink {

  implicit val lastUpdatedReads: Reads[DateTime] = uk.gov.hmrc.mongo.json.ReactiveMongoFormats.dateTimeRead
  implicit val lastUpdatedWrites: Writes[DateTime] = uk.gov.hmrc.mongo.json.ReactiveMongoFormats.dateTimeWrite
  implicit val accountLinkFormat: OFormat[AccountLink] = Json.format[AccountLink]

}

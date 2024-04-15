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
import utils.SpecBase

import java.time.LocalDateTime

class AccountLinksSpec extends SpecBase {

  "AccountLink" should {
    "should be able to assign data to an accountLink" in new Setup {
      val res = new AccountLink(sessionId, eori, false,
        accountNumber, accountStatus, accountStatusId, linkId, lastUpdated)

      res mustBe accountLink
      res.sessionId mustBe sessionId
      res.eori mustBe eori
      res.isNiAccount mustBe false
      res.accountNumber mustBe accountNumber
      res.accountStatus mustBe accountStatus
      res.accountStatusId mustBe accountStatusId
      res.linkId mustBe linkId
      res.lastUpdated mustBe lastUpdated
    }

    "should be created from a sessionCacheInstance" in new Setup {
      val sessCache = new SessionCacheAccountLink(eori, false, accountNumber, AccountStatusOpen,
        Option(DebitRejectedAccountClosedOrTransferred), linkId)

      val res = new AccountLink(sessionId, sessionCacheAccountLink = sessCache)

      res.sessionId mustBe sessionId
      res.eori mustBe eori
      res.isNiAccount mustBe false
      res.accountNumber mustBe accountNumber
      res.accountStatus mustBe accountStatus
      res.accountStatusId mustBe accountStatusId
      res.linkId mustBe linkId
    }
  }

  "AccountLinkWithoutDate" should {
    "should be able to assign data to an accountLink" in new Setup {

      val res = new AccountLinkWithoutDate(eori, false,
        accountNumber, datelessAccount, datelssStatus, linkId)

      res mustBe accountLinkWithoutDate
    }
  }

  "SessionCacheLink" should {
    "should be able to assign data to this SessionCache" in new Setup {

      val res: SessionCacheAccountLink = SessionCacheAccountLink(eori, isNiAccount = false, accountNumber,
        AccountStatusOpen, Option(DebitRejectedAccountClosedOrTransferred), linkId)

      res mustBe sessionCache
    }
  }
}

trait Setup {

  val sessionId: String = "someSessionId"
  val eori: EORI = "someEori"
  val accountNumber: String = "1234567"
  val accountStatus: CDSAccountStatus = AccountStatusOpen
  val datelessAccount: String = "someAccountStatus"
  val datelssStatus: Option[Int] = Option(1)
  val accountStatusId: Option[CDSAccountStatusId] = Option(DebitRejectedAccountClosedOrTransferred)
  val linkId: String = "someLinkId"
  val danId: String = "someDan"
  lazy val lastUpdated: LocalDateTime = LocalDateTime.now()

  val accountLink: AccountLink = AccountLink(sessionId, eori, isNiAccount = false,
    accountNumber, accountStatus, accountStatusId, linkId, lastUpdated)

  val accountLinkWithoutDate: AccountLinkWithoutDate = new AccountLinkWithoutDate(
    eori, false, accountNumber, datelessAccount, datelssStatus, linkId)

  val sessionCache: SessionCacheAccountLink = SessionCacheAccountLink(eori, isNiAccount = false, accountNumber,
    AccountStatusOpen, Option(DebitRejectedAccountClosedOrTransferred), linkId)
}

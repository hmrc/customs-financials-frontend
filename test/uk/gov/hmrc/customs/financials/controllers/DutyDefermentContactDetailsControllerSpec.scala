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

package uk.gov.hmrc.customs.financials.controllers

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import play.api.http.Status
import play.api.test.Helpers._
import play.api.{Application, inject}
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.domain.{DefermentAccountAvailable, _}
import uk.gov.hmrc.customs.financials.services.ContactDetailsService
import uk.gov.hmrc.customs.financials.utils.SpecBase

import scala.concurrent.Future

class DutyDefermentContactDetailsControllerSpec extends SpecBase {

  "account contact details" should {
    "return SEE_OTHER with the correct URL" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(ArgumentMatchers.eq(sessionId), ArgumentMatchers.eq(outputLinkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, "someEori", inputDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))
      when(mockContactDetailsService.getEncyptedDanWithStatus(ArgumentMatchers.eq(inputDan), ArgumentMatchers.eq(DefermentAccountAvailable.value)))
        .thenReturn(Future.successful(contactDetailsUrl))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentContactDetailsController.showContactDetails(inputLinkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe (Status.SEE_OTHER)
        redirectLocation(result).value mustBe contactDetailsUrl
      }
    }
  }

  trait Setup {
    val sessionId = "session_1234"
    val inputDan = "1234567"
    val contactDetailsUrl = "https://contact-details/1234567"
    val outputLinkId = "link_id"
    val inputLinkId = "link_id+open"
    val mockContactDetailsService = mock[ContactDetailsService]
    val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    val app: Application = application().overrides(
      inject.bind[ContactDetailsService].toInstance(mockContactDetailsService),
      inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
    ).build()
  }
}
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
import org.mockito.ArgumentMatchers.anyString
import play.api.inject
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.customs.financials.config.ErrorHandler
import uk.gov.hmrc.customs.financials.connectors.{CustomsFinancialsApiConnector, CustomsFinancialsSessionCacheConnector}
import uk.gov.hmrc.customs.financials.domain.{DefermentAccountAvailable, _}
import uk.gov.hmrc.customs.financials.services.{DataStoreService, DirectDebitService}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class DutyDefermentDirectDebitSetupControllerSpec extends SpecBase {

  "the page" should {
    "return SDDS error template when 500 returned from SDDS " in new Setup {
      when(mockSessionCacheConnector.retrieveSession(ArgumentMatchers.eq(sessionId), ArgumentMatchers.eq(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))
      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email("some@email.com"))))
      when(mockSDDSService.getDirectDebitSetupURL(any, any, eqTo(someDan), any)(any))
        .thenReturn(Future.failed(new InternalServerException("boom")))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentDirectDebitSetupController.setup(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        val errorHandler = app.injector.instanceOf[ErrorHandler]
        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe errorHandler.sddsErrorTemplate()(request).toString
      }
    }
  }

  "Direct debit setup, using datastore for verification" should {

    "return SDDS error template, when email is not verified" in new Setup {
      when(mockDataStoreService.getEmail(anyString)(any))
        .thenReturn(Future.successful(Left(UnverifiedEmail)))

      when(mockSessionCacheConnector.retrieveSession(ArgumentMatchers.eq(sessionId), ArgumentMatchers.eq(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentDirectDebitSetupController.setup(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        val errorHandler = app.injector.instanceOf[ErrorHandler]
        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsString(result) mustBe errorHandler.sddsErrorTemplate()(request).toString
      }
    }

    "return the setup page, when the email is verified" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(ArgumentMatchers.eq(sessionId), ArgumentMatchers.eq(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockSDDSService.getDirectDebitSetupURL(any, any, ArgumentMatchers.eq(someDan), any)(any))
        .thenReturn(Future.successful(directDebitUrl))

      when(mockDataStoreService.getEmail(anyString)(any))
        .thenReturn(Future.successful(Right(Email("test@test.com"))))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentDirectDebitSetupController.setup(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe directDebitUrl
      }
    }
  }

  trait Setup {
    val someDan = "1234567"
    val linkId = "link_id"
    val sessionId = "session_1234"
    val directDebitUrl = "https://direct-debit/start/1234567"

    val mockSDDSService = mock[DirectDebitService]
    val mockDataStoreService = mock[DataStoreService]
    val mockCustomsFinancialsApiConnector = mock[CustomsFinancialsApiConnector]
    val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    val app = application().overrides(
      inject.bind[DirectDebitService].toInstance(mockSDDSService),
      inject.bind[DataStoreService].toInstance(mockDataStoreService),
      inject.bind[CustomsFinancialsApiConnector].toInstance(mockCustomsFinancialsApiConnector),
      inject.bind[CustomsFinancialsSessionCacheConnector].toInstance((mockSessionCacheConnector))
    ).build()
  }

}

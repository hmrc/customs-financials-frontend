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

package controllers

import connectors.{CustomsFinancialsSessionCacheConnector, SdesConnector}
import domain._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.api.inject
import play.api.mvc.Results.Redirect
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HttpClient
import utils.SpecBase

import scala.concurrent.Future

class YourContactDetailsControllerSpec extends SpecBase {

  //TODO - These tests do not work and need refactoring.
  "YourContactDetailsController" should {
    "return OK" ignore new Setup {
      when[Future[String]](mockHttpClient.GET(any, any[Seq[(String, String)]],
        any[Seq[(String, String)]])(any, any, any)).thenReturn(Future.successful("Some_String"))

      val request = fakeRequest(GET, routes.YourContactDetailsController.onPageLoad().url)
      val result = route(app, request).value
      status(result) should be(OK)
    }

    "redirect to home page if no sessionId" ignore new Setup {
      when[Future[String]](mockHttpClient.GET(any, any[Seq[(String, String)]],
        any[Seq[(String, String)]])(any, any, any)).thenReturn(Future.failed(new RuntimeException("")))

      val request = fakeRequest(GET, routes.YourContactDetailsController.onPageLoad().url)
      val result = route(app, request).value
      redirectLocation(result).get mustBe Redirect(controllers.routes.CustomsFinancialsHomeController.index.url)
    }
  }

  trait Setup {

    val n1 = 200
    val n2 = 100
    val n3 = 50
    val n4 = 10

    val dd1 = DutyDefermentAccount("1231231231", newUser().eori, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)),
        Some(BigDecimal(n3)), Some(BigDecimal(n4))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd2 = DutyDefermentAccount("7567567567", newUser().eori, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd3 = DutyDefermentAccount("7897897897", "testEori10", AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)),
        Some(BigDecimal(n3)), Some(BigDecimal(n4))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd4 = DutyDefermentAccount("1112223334", "testEori11", AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val cashAccount1 = CashAccount("1000000", "testEori10", AccountStatusOpen,
      DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(n2))))

    val cashAccount2 = CashAccount("2000000", "testEori11", AccountStatusOpen,
      DefermentAccountAvailable, CDSCashBalance(None))

    val ggAccount1 = GeneralGuaranteeAccount("1234444", "testEori12", AccountStatusOpen,
      DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(n2*5), BigDecimal(n2*3))))

    val ggAccount2 = GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val cdsAccounts = CDSAccounts(newUser().eori, None, accounts)

    val mockApiService = mock[ApiService]
    val mockDataStoreService = mock[DataStoreService]
    val mockSdesConnector = mock[SdesConnector]
    val mockSessionCache = mock[CustomsFinancialsSessionCacheConnector]
    val mockHttpClient = mock[HttpClient]

    val email: Email = Email("email@123.com")

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any)).thenReturn(Future.successful(cdsAccounts))
    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
    when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(email)))

    val app = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector),
        inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCache)
      ).configure("features.new-agent-view-enabled" -> false).build()
  }
}

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

import connectors.{CustomsFinancialsSessionCacheConnector, SdesConnector, SecureMessageConnector}
import domain.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.{Application, inject}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.TestData.{TEST_ID, TEST_MESSAGE_BANNER}

import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import utils.{ShouldMatchers, SpecBase}

class YourContactDetailsControllerSpec extends SpecBase with ShouldMatchers {

  "YourContactDetailsController" should {
    "return OK when request session id is found in the cache" in new Setup {

      when(requestBuilder.execute(any[HttpReads[String]], any[ExecutionContext]))
        .thenReturn(Future.successful("Some_String"))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      when(mockSessionCache.getSessionId(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Option(HttpResponse(OK, sessionId))))

      when(mockSecureMessageConnector.getMessageCountBanner(any)(any)).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequestWithSession(GET, routes.YourContactDetailsController.onPageLoad().url, sessionId)

      val result: Future[Result] = route(app, request).value
      status(result) should be(OK)
    }

    "display the message banner on successful page load" in new Setup {
      val returnUrl = s"http://localhost:9876${controllers.routes.YourContactDetailsController.onPageLoad()}"

      when(requestBuilder.execute(any[HttpReads[String]], any[ExecutionContext]))
        .thenReturn(Future.successful("Some_String"))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      when(mockSessionCache.getSessionId(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Option(HttpResponse(OK, sessionId))))

      when(mockSecureMessageConnector.getMessageCountBanner(eqTo(returnUrl))(any))
        .thenReturn(Future.successful(Some(HtmlPartial.Success(Some(TEST_ID), TEST_MESSAGE_BANNER))))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequestWithSession(GET, routes.YourContactDetailsController.onPageLoad().url, sessionId)

      val result: Future[Result] = route(app, request).value
      status(result) should be(OK)

      val viewContent: String = contentAsString(result)
      shouldDisplayMessageBanner(viewContent)
    }

    "redirect to Home page if cache session id and request session id do not match" in new Setup {
      val sessionHeaderValue = "session_acf"

      when(requestBuilder.execute(any[HttpReads[String]], any[ExecutionContext]))
        .thenReturn(Future.successful("Some_String"))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      when(mockSessionCache.getSessionId(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Option(HttpResponse(OK, sessionId))))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequestWithSession(GET, routes.YourContactDetailsController.onPageLoad().url, sessionHeaderValue)

      val result: Future[Result] = route(app, request).value

      status(result)           should be(SEE_OTHER)
      redirectLocation(result) should be(Option(routes.CustomsFinancialsHomeController.index.url))
    }

    "redirect to Home page if there is no sessionId present" in new Setup {
      val sessionHeaderValue = "session_acf"

      when(requestBuilder.execute(any[HttpReads[String]], any[ExecutionContext]))
        .thenReturn(Future.successful("Some_String"))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      when(mockSessionCache.getSessionId(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequestWithSession(GET, routes.YourContactDetailsController.onPageLoad().url, sessionHeaderValue)

      val result: Future[Result] = route(app, request).value

      status(result)           should be(SEE_OTHER)
      redirectLocation(result) should be(Option(routes.CustomsFinancialsHomeController.index.url))
    }

    "redirect to home page if sessionId is not found in cache" in new Setup {
      when(requestBuilder.execute(any[HttpReads[String]], any[ExecutionContext]))
        .thenReturn(Future.successful("Some_String"))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      when(mockSessionCache.getSessionId(any)(any)).thenReturn(Future.successful(None))

      val request: FakeRequest[AnyContentAsEmpty.type] =
        fakeRequest(GET, routes.YourContactDetailsController.onPageLoad().url)

      val result: Future[Result] = route(app, request).value

      status(result)           should be(SEE_OTHER)
      redirectLocation(result) should be(Option(routes.CustomsFinancialsHomeController.index.url))
    }
  }

  private def shouldDisplayMessageBanner(view: String) = {
    view should include("Home")
    view should include("Messages")
    view should include("Your contact details")
    view should include("Your account authorities")
  }

  trait Setup {
    val sessionId = "session_acfe456"

    val n1 = 200
    val n2 = 100
    val n3 = 50
    val n4 = 10

    val dd1: DutyDefermentAccount = DutyDefermentAccount(
      "1231231231",
      newUser().eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)), Some(BigDecimal(n3)), Some(BigDecimal(n4))),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd2: DutyDefermentAccount = DutyDefermentAccount(
      "7567567567",
      newUser().eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd3: DutyDefermentAccount = DutyDefermentAccount(
      "7897897897",
      "testEori10",
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)), Some(BigDecimal(n3)), Some(BigDecimal(n4))),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd4: DutyDefermentAccount = DutyDefermentAccount(
      "1112223334",
      "testEori11",
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(n1)), Some(BigDecimal(n2)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val cashAccount1: CashAccount = CashAccount(
      "1000000",
      "testEori10",
      AccountStatusOpen,
      DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(n2)))
    )

    val cashAccount2: CashAccount =
      CashAccount("2000000", "testEori11", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(None))

    val ggAccount1: GeneralGuaranteeAccount = GeneralGuaranteeAccount(
      "1234444",
      "testEori12",
      AccountStatusOpen,
      DefermentAccountAvailable,
      Some(GeneralGuaranteeBalance(BigDecimal(n2 * 5), BigDecimal(n2 * 3)))
    )

    val ggAccount2: GeneralGuaranteeAccount =
      GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts: List[CDSAccount] = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val cdsAccounts: CDSAccounts   = CDSAccounts(newUser().eori, None, accounts)

    val mockApiService: ApiService                               = mock[ApiService]
    val mockDataStoreService: DataStoreService                   = mock[DataStoreService]
    val mockSdesConnector: SdesConnector                         = mock[SdesConnector]
    val mockSecureMessageConnector: SecureMessageConnector       = mock[SecureMessageConnector]
    val mockSessionCache: CustomsFinancialsSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]
    val mockHttpClient: HttpClientV2                             = mock[HttpClientV2]
    val requestBuilder: RequestBuilder                           = mock[RequestBuilder]
    val email: Email                                             = Email("email@123.com")

    when(mockSessionCache.getAccontLinks(any)(any)).thenReturn(Future.successful(Option(Seq())))

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
      .thenReturn(Future.successful(cdsAccounts))

    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

    when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(email)))

    when(mockDataStoreService.getOwnCompanyName(any)).thenReturn(Future.successful(Some("companyName")))

    when(mockDataStoreService.getCompanyAddress(any))
      .thenReturn(Future.successful(Option(CompanyAddress(emptyString, emptyString, None, "GB"))))

    val app: Application = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector),
        inject.bind[SecureMessageConnector].toInstance(mockSecureMessageConnector),
        inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCache)
      )
      .configure("features.new-agent-view-enabled" -> false)
      .build()
  }
}

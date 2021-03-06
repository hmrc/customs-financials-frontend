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

package controllers

import connectors.SdesConnector
import domain.{Account, AccountStatusOpen, AuthorisedBalances, AuthorisedCashAccount, AuthorisedDutyDefermentAccount, AuthorisedGeneralGuaranteeAccount, AuthorizedToViewPageState, CDSAccounts, CDSCashBalance, CashAccount, DefermentAccountAvailable, DutyDefermentAccount, DutyDefermentBalance, GeneralGuaranteeAccount, GeneralGuaranteeBalance, NoAuthorities, SearchError, SearchedAuthorities}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.{Application, inject}
import play.api.test.Helpers._
import services.{ApiService, DataStoreService, SdesService}
import uk.gov.hmrc.http.GatewayTimeoutException
import utils.SpecBase

import scala.concurrent.Future

class AuthorizedToViewControllerSpec extends SpecBase {

  "The Authorized to View page" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        status(result) should be(OK)
      }
    }

    "show the search EORI view when the feature flag is enabled" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
      val newApp: Application = application().overrides(
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> true).build()
      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(newApp, request).value
        status(result) should be(OK)
      }
    }
  }

  "The Authorized to View download CSV page" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        status(result) should be(OK)
      }
    }

    "download authorities csv page when requests all accounts" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
      val newApp: Application = application().overrides(
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("microservice.services.sdes.context" -> true).build()
      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(newApp, request).value
        status(result) should be(OK)
      }
    }
  }


    "onSubmit" should {
    "return OK if there are authorities returned" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some(AuthorisedBalances("10.0", "10.0")))
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))
      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB123456789012")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        html.text().contains("Search results for GB123456789012") shouldBe true
      }
    }

    "return OK if there are no authorities returned and display the no authorities page" in new Setup {
      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(NoAuthorities)))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB123456789012")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        html.text().contains("There are no matching result for 'GB123456789012'") shouldBe true
      }
    }

    "return BAD_REQUEST if an invalid payload sent" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "ERROR")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return internal server error when there is an error from the API" in new Setup {
      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(SearchError)))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB123456789012")
        val result = route(app, request).value
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "The header section" should {
    "have a back to accounts link on top" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.containsLinkWithText("/customs/payment-records", "link-back")
      }
    }

    "have a heading field" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByTag("h1").text mustBe "Find accounts you have authority to use"
      }
    }
  }

  "The accounts section" should {

    "have a list of duty deferment accounts that I do not own along with their EORI and Account number displayed" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementById("account-0").text mustBe s"Account: ${dd3.number}"
        html.getElementById("eori-0").text mustBe s"EORI number: ${dd3.owner}"
        html.getElementById("account-1").text mustBe s"Account: ${dd4.number}"
        html.getElementById("eori-1").text mustBe s"EORI number: ${dd4.owner}"
      }
    }

    "list duty deferment balances only when they are available" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByClass("guaranteeLimitRemaining").text mustBe "Guarantee limit remaining: ??50"
        html.getElementsByClass("accountLimitRemaining").text mustBe "Account limit remaining: ??20"
      }
    }

    "have a list of cash accounts that I do not own along with their EORI and Account number displayed" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementById("account-2").text mustBe s"Account: ${cashAccount1.number}"
        html.getElementById("eori-2").text() shouldBe (s"EORI number: ${cashAccount1.owner}")
        html.getElementById("account-3").text mustBe s"Account: ${cashAccount2.number}"
        html.getElementById("eori-3").text() shouldBe (s"EORI number: ${cashAccount2.owner}")
      }
    }

    "list cash balances only when they are available" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByClass("availableAccountBalance").text mustBe "Available account balance ??100"
      }
    }

    "have a list of guarantee accounts that I do not own along with their EORI and Account number displayed" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementById("account-4").text mustBe s"Account: ${ggAccount1.number}"
        html.getElementById("eori-4").text() shouldBe (s"EORI number: ${ggAccount1.owner}")
        html.getElementById("account-5").text mustBe s"Account: ${ggAccount2.number}"
        html.getElementById("eori-5").text() shouldBe (s"EORI number: ${ggAccount2.owner}")
      }
    }

    "list guarantee balances only when they are available" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByClass("availableGuaranteeBalance").text mustBe "Guarantee limit remaining ??300"
      }
    }

    "not display account, account type and eori headings when there are no accounts" in {
      val cdsAccountsEmpty = CDSAccounts(newUser().eori, List.empty)
      val mockApiService = mock[ApiService]
      val mockSdesConnector = mock[SdesConnector]
      val state = AuthorizedToViewPageState(1)

      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
        .thenReturn(Future.successful(cdsAccountsEmpty))

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val app = application()
        .overrides(
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[SdesConnector].toInstance(mockSdesConnector)
        ).build()

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.notContainElementById("account_0")
        html.notContainElementById("account-type-0")
        html.notContainElementById("eori_0")
      }
    }

    "display Account unavailable page when GatewayTimeoutException is thrown" in {
      val mockApiService = mock[ApiService]
      val state = AuthorizedToViewPageState(1)

      when(mockApiService.getAccounts(any)(any))
        .thenReturn(Future.failed(new GatewayTimeoutException("Request Timeout")))
      val app = application()
        .overrides(
          inject.bind[ApiService].toInstance(mockApiService)
        ).configure("features.new-agent-view-enabled" -> false).build()
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad(state).url)
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.CustomsFinancialsHomeController.showAccountUnavailable.url
      }
    }
  }

  trait Setup {

    val state: AuthorizedToViewPageState = AuthorizedToViewPageState(1)

    val dd1 = DutyDefermentAccount("1231231231", newUser().eori, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)
    val dd2 = DutyDefermentAccount("7567567567", newUser().eori, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), None, None), viewBalanceIsGranted = true, isIsleOfMan = false)
    val dd3 = DutyDefermentAccount("7897897897", "testEori10", AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)
    val dd4 = DutyDefermentAccount("1112223334", "testEori11", AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)), None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val cashAccount1 = CashAccount("1000000", "testEori10", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(100))))
    val cashAccount2 = CashAccount("2000000", "testEori11", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(None))

    val ggAccount1 = GeneralGuaranteeAccount("1234444", "testEori12", AccountStatusOpen, DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(500), BigDecimal(300))))
    val ggAccount2 = GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val cdsAccounts = CDSAccounts(newUser().eori, accounts)

    val mockApiService = mock[ApiService]
    val mockDataStoreService = mock[DataStoreService]
    val mockSdesConnector = mock[SdesConnector]

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
      .thenReturn(Future.successful(cdsAccounts))
    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

    val app = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> false).build()
  }
}


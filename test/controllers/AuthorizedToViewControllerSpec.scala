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

import connectors.SdesConnector
import domain._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.any
import play.api.test.Helpers._
import play.api.{Application, inject}
import services.{ApiService, DataStoreService}
import utils.SpecBase

import scala.concurrent.Future
import scala.reflect.io.File

class AuthorizedToViewControllerSpec extends SpecBase {

  "The Authorized to View page" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
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
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(newApp, request).value
        status(result) should be(OK)
      }
    }
  }

  "The Authorized to View download CSV page" should {
    "return OK" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
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
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(newApp, request).value
        status(result) should be(OK)
      }
    }

    "getCsvFile() sort by file name" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val fileObj1 = File("fileserving1.csv")
      val fileObj2 = File("fileserving2.csv")
      val fileObj3 = File("fileserving3.csv")
      val fileObj4 = File("fileserving4.csv")
      val fileObj5 = File("fileserving5.csv")

      val fileObjectList = List(fileObj1, fileObj2)

      fileObjectList.sortWith((x1, x2) => x1.lastModified < x2.lastModified)

      val filesWithNames = List("CS_000000000154_csv.csv",
        "CS_000000000152_csv.csv", "CS_000000000153_csv.csv", "CS_000000000151_csv.csv")
      val filesseperated = filesWithNames.map(x => x.split("_")(1))

      val filesSorted = filesseperated.sortWith(_ < _)

      filesseperated.sortWith(_ < _).headOption

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value
        status(result) should be(OK)
      }
    }
  }

  "onSubmit" should {
    "return OK if there are authorities returned" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some(AuthorisedBalances("100.0", "200.0")))
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
        html.text().contains("£100.0") shouldBe true
        html.text().contains("£200.0") shouldBe true
      }
    }

    "return OK if there are authorities returned with spaces in search string" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some(AuthorisedBalances("1000.0", "0.0")))
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))
      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB 12 3456 789 012")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        html.text().contains("Search results for GB123456789012") shouldBe true
        html.text().contains("£1000.00") shouldBe false
      }
    }

    "return OK if there are no authorities returned and display the no authorities page" in new Setup {
      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(NoAuthorities)))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB 12 34 56 78 90 12")
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

    "Display error message if searching your own EORI number" in new Setup {
      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> newUser().eori)

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe BAD_REQUEST
        html.text().contains("You cannot search your own EORI number") shouldBe true
      }
    }

    "Display error message if searching your own account number" in new Setup {
      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> accounts.map(_.number).head)

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe BAD_REQUEST
        html.text().contains("You cannot search your own account number") shouldBe true
      }
    }
  }

  "The header section" should {
    "have a back to accounts link on top" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        html.containsLinkWithText("/customs/payment-records", "link-back")
      }
    }

    "have a heading field" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        println(html.getElementsByTag("h1").text)
      }
    }
  }

  trait Setup {

    val dd1 = DutyDefermentAccount("1231231231", newUser().eori, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd2 = DutyDefermentAccount("7567567567", newUser().eori, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd3 = DutyDefermentAccount("7897897897", "testEori10", AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd4 = DutyDefermentAccount("1112223334", "testEori11", AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val cashAccount1 = CashAccount("1000000", "testEori10", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(100))))
    val cashAccount2 = CashAccount("2000000", "testEori11", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(None))

    val ggAccount1 = GeneralGuaranteeAccount("1234444", "testEori12", AccountStatusOpen,
      DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(500), BigDecimal(300))))

    val ggAccount2 = GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val cdsAccounts = CDSAccounts(newUser().eori, accounts)

    val mockApiService = mock[ApiService]
    val mockDataStoreService = mock[DataStoreService]
    val mockSdesConnector = mock[SdesConnector]

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any)).thenReturn(Future.successful(cdsAccounts))
    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

    val app = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> false).build()
  }
}

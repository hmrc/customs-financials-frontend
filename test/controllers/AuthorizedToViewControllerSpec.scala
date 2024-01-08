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
import domain.FileFormat.Csv
import domain.FileRole.StandingAuthority
import domain._
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers._
import play.api.{Application, inject}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.Future
import scala.reflect.io.File

class AuthorizedToViewControllerSpec extends SpecBase {

  "The Authorized to View page" should {
    "return OK" in new Setup {
      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))

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

    "display the search EORI view with GB authority link when there are only GB authorities' csv file" in new Setup {

      val authCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(authCsvFiles))

      val newApp: Application = application().overrides(
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> true).build()
      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(newApp, request).value
        status(result) should be(OK)

        val html = Jsoup.parse(contentAsString(result))

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        intercept[RuntimeException] {
          html.getElementById("xi-csv-authority-link").attr("href")
        }
      }
    }

    "display the search EORI view with GB and XI authority link when there are" +
      "GB and XI authorities' csv files" in new Setup {

      val suthCsvFiles: Seq[StandingAuthorityFile] =
        Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(suthCsvFiles))

      val newApp: Application = application().overrides(
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> true).build()
      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(newApp, request).value
        status(result) should be(OK)

        val html = Jsoup.parse(contentAsString(result))

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        html.getElementById("xi-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.xi-authority")
        html.getElementById("xi-csv-authority-link").attr("href") mustBe xiStanAuthFile154Url
      }
    }

    "return OK when correct email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value

        status(result) should be(OK)
      }
    }

    "redirected to email undeliverable page when undeliverable email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Left(UndeliverableEmail(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value

        status(result) should be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.EmailController.showUndeliverable().url)
      }
    }

    "redirected to email unverified page when unverified email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value

        status(result) should be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.EmailController.showUnverified().url)
      }
    }
  }

  "The Authorized to View download CSV page" should {
    "return OK" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))

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

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val fileObj1 = File("CS_000000000154_csv.csv")
      val fileObj2 = File("CS_000000000152_csv.csv")

      val fileObjectList = List(fileObj1, fileObj2)

      fileObjectList.sortWith((x1, x2) => x1.lastModified < x2.lastModified)

      val filesWithNames = List("CS_000000000154_csv.csv",
        "CS_000000000152_csv.csv", "CS_000000000153_csv.csv", "CS_000000000151_csv.csv")
      val filesseperated = filesWithNames.map(x => x.split("_")(1))

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

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

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
      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

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
      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB 12 34 56 78 90 12")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) shouldBe OK
        html.text().contains("There are no matching result for 'GB123456789012'") shouldBe true
      }
    }

    "return OK if there are authorities returned for both GB and XI EORI and both SearchAuthorities " +
      "have no balance" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Right(SearchedAuthorities("3",
          Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))
        .andThenAnswer(Future.successful(Right(SearchedAuthorities("3",
          Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))

      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Option("XI123456789")))

      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> "GB123456789012")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        html.text().contains("Search results for GB123456789012") shouldBe true
      }
    }

    "return OK if there are no authorities returned for both GB/XI EORI for a account and" +
      " display no authorities page" in new Setup {

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Option("XI123456789")))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(NoAuthorities))).andThenAnswer(Future.successful(Left(NoAuthorities)))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "1000000")

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        html.text().contains("There are no matching result for '1000000'") shouldBe true
      }
    }

    "return OK if there is XI EORI associated with the GB EORI and authorities are returned for account" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"),
          Some(AuthorisedBalances("100.0", "200.0")))
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(
          Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))
      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Option("XI123456789")))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "1234567")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK

        html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
        html.text().contains("£100.0") shouldBe true
        html.text().contains("£200.0") shouldBe true
        html.text().contains(messages(app)("cf.search.authorities.result.xiEori.number")) shouldBe true
      }
    }

    "return OK if there is XI EORI associated with the GB EORI and authorities are returned for " +
      "GB EORI but not for XI EORI for an account number" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"),
          Some(AuthorisedBalances("100.0", "200.0")))
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(
          Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))).andThenAnswer(
          Future.successful(Left(NoAuthorities))
        )
      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Option("XI123456789")))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "1234567")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK

        html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
        html.text().contains("£100.0") shouldBe true
        html.text().contains("£200.0") shouldBe true
        html.text().contains(messages(app)("cf.search.authorities.result.eori.number")) shouldBe true
      }
    }

    "return OK if there is XI EORI associated with the GB EORI and authorities are returned for " +
      "XI EORI but not for GB EORI for an account number" in new Setup {
      val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
        AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))
      val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
        AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"),
          Some(AuthorisedBalances("100.0", "200.0")))
      val cashAccount: AuthorisedCashAccount =
        AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(NoAuthorities))).andThenAnswer(Future.successful(
          Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount)))))

      when(mockDataStoreService.getCompanyName(any)(any))
        .thenReturn(Future.successful(Some("Company name")))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Option("XI123456789")))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "1234567")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK

        html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
        html.text().contains("£100.0") shouldBe true
        html.text().contains("£200.0") shouldBe true
        html.text().contains(messages(app)("cf.search.authorities.result.xiEori.number")) shouldBe true
      }
    }

    "return BAD_REQUEST if an invalid payload sent" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
      running(app) {
        val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> "ERROR")

        val result = route(app, request).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST with correct CSV links for GB authorities if an invalid payload sent" in new Setup {

      val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))
      running(app) {
        val request = fakeRequest(
          POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "ERROR")

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        intercept[RuntimeException] {
          html.getElementById("xi-csv-authority-link").attr("href")
        }
      }
    }

    "return BAD_REQUEST with correct CSV links for both GB and XI authorities " +
      "if an invalid payload sent" in new Setup {

      val authCsvFiles: Seq[StandingAuthorityFile] =
        Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(authCsvFiles))
      running(app) {
        val request = fakeRequest(
          POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "ERROR")

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        html.getElementById("xi-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.xi-authority")
        html.getElementById("xi-csv-authority-link").attr("href") mustBe xiStanAuthFile154Url
      }
    }

    "return BAD_REQUEST with correct error msg when agent is not registered for his own XI EORI" +
      " and search authority using trader's XI EORI" in new Setup {

      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq()))
      when(mockDataStoreService.getXiEori(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(
          POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "XI123456789012")

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        html.text().contains(messages(app)("cf.search.authorities.error.register-xi-eori"))
      }
    }

    "return Internal Server Error and go to view search no result page when there are errors from the API while" +
      "retrieving authorities for GB and XI EORI for EORI" in new Setup {
      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(SearchError))).andThenAnswer(Future.successful(Left(SearchError)))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "GB12345678")
        val result = route(app, request).value
        status(result) shouldBe 500
      }
    }

    "return Internal Server Error when there are errors from the API while" +
      "retrieving authorities for GB and XI EORI for input account number" in new Setup {
      when(mockApiService.searchAuthorities(any, any)(any))
        .thenReturn(Future.successful(Left(SearchError))).andThenAnswer(Future.successful(Left(SearchError)))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "1000000")
        val result = route(app, request).value
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "Display error message if searching your own EORI number" in new Setup {
      val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> newUser().eori)

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        html.text().contains("You cannot search your own EORI number") shouldBe true
      }
    }

    "Display error message if searching your own XI EORI number" in new Setup {
      val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Some("XI123456789912")))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> "XI123456789912")

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        html.getElementById("gb-csv-authority-link").html() mustBe
          messages(app)("cf.authorities.notification-panel.a.gb-authority")
        html.getElementById("gb-csv-authority-link").attr("href") mustBe gbStanAuthFile154Url

        html.text().contains("You cannot search your own EORI number") shouldBe true
      }
    }

    "Display error message if searching your own account number" in new Setup {
      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))
      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
        .thenReturn(Future.successful(cdsAccounts))
      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().xiEori.get))(any))
        .thenReturn(Future.successful(xiCdsAccounts))

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

    "Display error message if searching your own XI DD account number" in new Setup {
      when(mockDataStoreService.getXiEori(any)(any))
        .thenReturn(Future.successful(Some("XI123456789012")))
      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
        .thenReturn(Future.successful(cdsAccounts))
      when(mockApiService.getAccounts(ArgumentMatchers.eq("XI123456789012"))(any))
        .thenReturn(Future.successful(xiCdsAccounts))

      running(app) {
        val request = fakeRequest(POST,
          routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody(
          "value" -> xiAccounts.map(_.number).head)

        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        html.text().contains("You cannot search your own account number") shouldBe true
      }
    }
  }

  "The header section" should {
    "have a back to accounts link on top" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.containsLinkWithText("/customs/payment-records", "link-back")
      }
    }

    "have a heading field" in new Setup {

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementById("h1") must not be emptyString
      }
    }
  }

  trait Setup {

    val dd1 = DutyDefermentAccount("1231231231", newUser().eori, false, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd2 = DutyDefermentAccount("7567567567", newUser().eori, false, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd3 = DutyDefermentAccount("7897897897", "testEori10", false, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val dd4 = DutyDefermentAccount("1112223334", "testEori11", false, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        None, None), viewBalanceIsGranted = true, isIsleOfMan = false)

    val xiDd = DutyDefermentAccount("1231231000", newUser().xiEori.get, true, AccountStatusOpen,
      DefermentAccountAvailable, DutyDefermentBalance(Some(BigDecimal(200)), Some(BigDecimal(100)),
        Some(BigDecimal(50)), Some(BigDecimal(20))), viewBalanceIsGranted = true, isIsleOfMan = false)

    val cashAccount1 = CashAccount("1000000", "testEori10", AccountStatusOpen, DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(100))))

    val cashAccount2 = CashAccount("2000000", "testEori11", AccountStatusOpen, DefermentAccountAvailable,
      CDSCashBalance(None))

    val ggAccount1 = GeneralGuaranteeAccount("1234444", "testEori12", AccountStatusOpen,
      DefermentAccountAvailable, Some(GeneralGuaranteeBalance(BigDecimal(500), BigDecimal(300))))

    val ggAccount2 = GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val xiAccounts = List(xiDd)

    val cdsAccounts = CDSAccounts(newUser().eori, None, accounts)
    val xiCdsAccounts = CDSAccounts(newUser().xiEori.get, None, xiAccounts)
    val emptyCdsAccounts = CDSAccounts(newUser().eori, None, List())

    val gbEORI = "GB123456789012"
    val xiEORI = "XI123456789012"

    val gbStanAuthFile153Url = "https://test.co.uk/GB123456789012/SA_000000000153_csv.csv"
    val gbStanAuthFile154Url = "https://test.co.uk/GB123456789012/SA_000000000154_csv.csv"
    val xiStanAuthFile153Url = "https://test.co.uk/XI123456789012/SA_000000000153_XI_csv.csv"
    val xiStanAuthFile154Url = "https://test.co.uk/XI123456789012/SA_000000000154_XI_csv.csv"

    val standAuthMetadata: StandingAuthorityMetadata = StandingAuthorityMetadata(2022, 6, 1, Csv, StandingAuthority)

    val gbStandingAuth1: StandingAuthorityFile = StandingAuthorityFile(
      "SA_000000000153_csv.csv", gbStanAuthFile153Url, 500L, standAuthMetadata, gbEORI)
    val gbStandingAuth2: StandingAuthorityFile = StandingAuthorityFile(
      "SA_000000000154_csv.csv", gbStanAuthFile154Url, 500L, standAuthMetadata, gbEORI)

    val xiStandingAuth1: StandingAuthorityFile = StandingAuthorityFile(
      "SA_XI_000000000153_csv.csv", xiStanAuthFile153Url, 500L, standAuthMetadata, xiEORI)
    val xiStandingAuth2: StandingAuthorityFile = StandingAuthorityFile(
      "SA_XI_000000000154_XI_csv.csv", xiStanAuthFile154Url, 500L, standAuthMetadata, xiEORI)

    val emailId = "test@test.com"

    val mockApiService: ApiService = mock[ApiService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]
    val mockSdesConnector: SdesConnector = mock[SdesConnector]

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
      .thenReturn(Future.successful(cdsAccounts))
    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().xiEori.get))(any))
      .thenReturn(Future.successful(xiCdsAccounts))
    when(mockApiService.getAccounts(ArgumentMatchers.anyString())(any))
      .thenReturn(Future.successful(emptyCdsAccounts))
    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any))
      .thenReturn(Future.successful(Seq.empty))

    val app = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      ).configure("features.new-agent-view-enabled" -> false).build()
  }
}

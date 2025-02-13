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
import domain.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.Helpers.*
import play.api.{Application, inject}
import services.{ApiService, DataStoreService}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HeaderCarrier
import utils.{ShouldMatchers, SpecBase}
import utils.TestData.{
  BALANCE_100, BALANCE_20, BALANCE_200, BALANCE_300, BALANCE_50, BALANCE_500, DAY_1, FILE_SIZE_500, MONTH_6, YEAR_2022
}

import scala.concurrent.Future

class AuthorizedToViewControllerSpec extends SpecBase with ShouldMatchers {

  "The Authorized to View page" should {
    "return OK" in new Setup {
      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value
        status(result) should be(OK)
      }
    }

    "show the search EORI view when the feature flag is enabled" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val newApp: Application = application()
        .overrides(
          inject.bind[SdesConnector].toInstance(mockSdesConnector)
        )
        .configure("features.new-agent-view-enabled" -> true)
        .build()

      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(newApp, request).value
        status(result) should be(OK)
      }
    }

    "display the search EORI view with GB authority link " +
      "when there are only GB authorities' csv file " +
      "and authorities-notification-panel-enabled feature flag is true" in new Setup {

        val authCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(authCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> true,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        running(newApp) {
          val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
          val result  = route(newApp, request).value
          status(result) should be(OK)

          val html = Jsoup.parse(contentAsString(result))

          html.getElementById("gb-csv-authority-link").html()       shouldBe
            messages(app)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href") shouldBe gbStanAuthFile154Url

          intercept[RuntimeException] {
            html.getElementById("xi-csv-authority-link").attr("href")
          }
        }
      }

    "display the search EORI view with GB and XI authority link " +
      "when there are GB and XI authorities' csv files " +
      "and authorities-notification-panel-enabled feature flag is true" in new Setup {

        val suthCsvFiles: Seq[StandingAuthorityFile] =
          Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(suthCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> true,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        running(newApp) {
          val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
          val result  = route(newApp, request).value
          status(result) should be(OK)

          val html = Jsoup.parse(contentAsString(result))

          html.getElementById("gb-csv-authority-link").html()       shouldBe
            messages(app)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href") shouldBe gbStanAuthFile154Url

          html.getElementById("xi-csv-authority-link").html()       shouldBe
            messages(app)("cf.authorities.notification-panel.a.xi-authority")
          html.getElementById("xi-csv-authority-link").attr("href") shouldBe xiStanAuthFile154Url
        }
      }

    "display the search EORI view without GB and XI authority link " +
      "when authorities-notification-panel-enabled feature flag is false" in new Setup {

        val suthCsvFiles: Seq[StandingAuthorityFile] =
          Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(suthCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> true,
            "features.authorities-notification-panel-enabled" -> false
          )
          .build()

        running(newApp) {
          val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
          val result  = route(newApp, request).value
          status(result) should be(OK)

          val html = Jsoup.parse(contentAsString(result))

          Option(html.getElementById("gb-csv-authority-link")) shouldBe None
          Option(html.getElementById("xi-csv-authority-link")) shouldBe None
        }
      }

    "return OK when correct email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value

        status(result) should be(OK)
      }
    }

    "redirected to email undeliverable page when undeliverable email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Left(UndeliverableEmail(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value

        status(result)             should be(SEE_OTHER)
        redirectLocation(result) shouldBe Some(controllers.routes.EmailController.showUndeliverable().url)
      }
    }

    "redirected to email unverified page when unverified email is returned from dataStoreService" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value

        status(result)             should be(SEE_OTHER)
        redirectLocation(result) shouldBe Some(controllers.routes.EmailController.showUnverified().url)
      }
    }
  }

  "The Authorized to View download CSV page" should {
    "return OK" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value

        status(result) should be(OK)
      }
    }

    "download authorities csv page when requests all accounts" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val newApp: Application = application()
        .overrides(
          inject.bind[SdesConnector].toInstance(mockSdesConnector)
        )
        .configure("microservice.services.sdes.context" -> true)
        .build()

      running(newApp) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(newApp, request).value

        status(result) should be(OK)
      }
    }

    "getCsvFile() sort by file name" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))

      val filesWithNames: List[EORI] =
        List("CS_000000000154_csv.csv", "CS_000000000152_csv.csv", "CS_000000000153_csv.csv", "CS_000000000151_csv.csv")
      val filesseperated: List[EORI] = filesWithNames.map(x => x.split("_")(1))

      filesseperated.sortWith(_ < _).headOption

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value
        status(result) should be(OK)
      }
    }
  }

  private def validateRedirectToOnSearchAndThen(eori: String, app: Application)(
    assertResultFn: (Future[Result], Document) => Unit
  ): Unit =
    running(app) {
      val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url)
        .withFormUrlEncodedBody("value" -> eori)

      val result = route(app, request).value

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe routes.AuthorizedToViewController.onSearch(eori).url

      val redirectRequest = fakeRequest(GET, routes.AuthorizedToViewController.onSearch(eori).url)
      val redirectResult  = route(app, redirectRequest).value
      val redirectHtml    = Jsoup.parse(contentAsString(redirectResult))

      assertResultFn(redirectResult, redirectHtml)
    }

  private def validateRedirectToOnNoSearchResultsAndThen(eori: String, app: Application)(
    assertResultFn: (Future[Result], Document) => Unit
  ): Unit =
    running(app) {
      val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url)
        .withFormUrlEncodedBody("value" -> eori)

      val result = route(app, request).value

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe routes.AuthorizedToViewController.onSearch(eori).url

      val firstRedirectRequest = fakeRequest(GET, routes.AuthorizedToViewController.onSearch(eori).url)
      val firstRedirectResult  = route(app, firstRedirectRequest).value

      val strippedEori = eori.replaceAll(" ", "")

      status(firstRedirectResult)                 shouldBe SEE_OTHER
      redirectLocation(firstRedirectResult).value shouldBe routes.AuthorizedToViewController
        .onNoSearchResult(strippedEori)
        .url

      val secondRedirectRequest = fakeRequest(GET, routes.AuthorizedToViewController.onNoSearchResult(strippedEori).url)
      val secondRedirectResult  = route(app, secondRedirectRequest).value
      val secondRedirectHtml    = Jsoup.parse(contentAsString(secondRedirectResult))

      assertResultFn(secondRedirectResult, secondRedirectHtml)
    }

  "onSubmit" should {
    "return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQuery should return OK if there are authorities returned" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(
            Account("1234", "GeneralGuarantee", "GB000000000000"),
            Some(AuthorisedBalances("100.0", "200.0"))
          )

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )

        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        validateRedirectToOnSearchAndThen(gbEORI, app) { (result, html) =>

          status(result) shouldBe OK

          html.text().contains(s"Search results for $gbEORI") shouldBe true
          html.text().contains("£100.0")                      shouldBe true
          html.text().contains("£200.0")                      shouldBe true
        }
      }

    "return SEE_OTHER if there are authorities returned with spaces in search string " +
      "and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQuery should return OK if there are authorities returned" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(
            Account("1234", "GeneralGuarantee", "GB000000000000"),
            Some(AuthorisedBalances("1000.0", "0.0"))
          )

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )

        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        validateRedirectToOnSearchAndThen("GB 12 3456 789 012", app) { (result, html) =>
          status(result)                                      shouldBe OK
          html.text().contains(s"Search results for $gbEORI") shouldBe true
          html.text().contains("£1000.0")                     shouldBe true
        }
      }

    "return SEE_OTHER if there are no authorities returned and " +
      "redirect to /authorities-search-results:searchQuery " +
      "call to /authorities-search-results:searchQuery should redirect to " +
      "/authorities-no-results:searchQuery with eori spaces removed " +
      "finally a call to this url should return no search result " in new Setup {

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(Future.successful(Left(NoAuthorities)))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        validateRedirectToOnNoSearchResultsAndThen("GB 12 34 56 78 90 12", app) { (result, html) =>
          status(result)                                      shouldBe OK
          html.text().contains(s"Search results for $gbEORI") shouldBe true
        }
      }

    " return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQuery should return OK if there are authorities returned " +
      "for both GB and XI EORI and both SearchAuthorities " + "have no balance" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), None)

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )

        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Option("XI123456789")))

        validateRedirectToOnSearchAndThen(gbEORI, app) { (result, html) =>
          status(result)                                      shouldBe OK
          html.text().contains(s"Search results for $gbEORI") shouldBe true
        }
      }

    "return SEE_OTHER if there are no authorities returned for both GB/XI EORI for a account and" +
      "redirect to /authorities-search-results:searchQuery " +
      "call to /authorities-search-results:searchQuery should redirect to " +
      "/authorities-no-results:searchQuery " +
      "finally a call to this url should return no search result" in new Setup {

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Option("XI123456789")))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(Future.successful(Left(NoAuthorities)))
          .thenReturn(Future.successful(Left(NoAuthorities)))

        validateRedirectToOnNoSearchResultsAndThen(gbEORI, app) { (result, html) =>
          status(result)                                      shouldBe OK
          html.text().contains(s"Search results for $gbEORI") shouldBe true
        }
      }

    "return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQuery should return OK if there is XI EORI associated " +
      "with the GB EORI and if there are authorities returned" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(
            Account("1234", "GeneralGuarantee", "GB000000000000"),
            Some(AuthorisedBalances("100.0", "200.0"))
          )

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )
        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Option("XI123456789")))

        validateRedirectToOnSearchAndThen("1234567", app) { (result, html) =>
          status(result)                                                                       shouldBe OK
          html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
          html.text().contains("£100.0")                                                       shouldBe true
          html.text().contains("£200.0")                                                       shouldBe true
        }
      }

    "return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQuery should return OK if there is XI EORI associated " +
      "with the GB EORI but not for XI EORI for an account number" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(
            Account("1234", "GeneralGuarantee", "GB000000000000"),
            Some(AuthorisedBalances("100.0", "200.0"))
          )

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )
          .thenReturn(
            Future.successful(Left(NoAuthorities))
          )
        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Option("XI123456789")))

        validateRedirectToOnSearchAndThen("1234567", app) { (result, html) =>
          status(result)                                                                       shouldBe OK
          html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
          html.text().contains("£100.0")                                                       shouldBe true
          html.text().contains("£200.0")                                                       shouldBe true
          html.text().contains(messages(app)("cf.search.authorities.result.eori.number"))      shouldBe true
        }
      }

    "return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      " and /authorities-search-results:searchQuery should return OK if there is XI EORI associated " +
      " with the GB EORI and authorities are returned for " +
      " XI EORI but not for GB EORI for an account number" in new Setup {
        val guaranteeAccount: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val dutyDefermentAccount: AuthorisedDutyDefermentAccount =
          AuthorisedDutyDefermentAccount(
            Account("1234", "GeneralGuarantee", "GB000000000000"),
            Some(AuthorisedBalances("100.0", "200.0"))
          )

        val cashAccount: AuthorisedCashAccount =
          AuthorisedCashAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(Future.successful(Left(NoAuthorities)))
          .thenReturn(
            Future.successful(Right(SearchedAuthorities("3", Seq(guaranteeAccount, dutyDefermentAccount, cashAccount))))
          )

        when(mockDataStoreService.getCompanyName(any)(any))
          .thenReturn(Future.successful(Some("Company name")))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Option("XI123456789")))

        validateRedirectToOnSearchAndThen("1234567", app) { (result, html) =>
          status(result)                                                                       shouldBe OK
          html.text().contains(messages(app)("cf.search.authorities.result.title", "1234567")) shouldBe true
          html.text().contains("£100.0")                                                       shouldBe true
          html.text().contains("£200.0")                                                       shouldBe true
          html.text().contains(messages(app)("cf.search.authorities.result.xiEori.number"))    shouldBe true
        }
      }

    "return BAD_REQUEST if an invalid payload sent" in new Setup {
      when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq.empty))
      running(app) {
        val request =
          fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url).withFormUrlEncodedBody("value" -> "ERROR")

        val result = route(app, request).value
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return BAD_REQUEST with correct CSV links for GB authorities " +
      "if an invalid payload sent " +
      "when authorities-notification-panel-enabled feature flag is true" in new Setup {

        val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        running(newApp) {
          val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> "ERROR")

          val result = route(newApp, request).value
          val html   = Jsoup.parse(contentAsString(result))

          status(result) shouldBe BAD_REQUEST

          html.getElementById("gb-csv-authority-link").html()       shouldBe
            messages(newApp)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href") shouldBe gbStanAuthFile154Url

          intercept[RuntimeException] {
            html.getElementById("xi-csv-authority-link").attr("href")
          }
        }
      }

    "return BAD_REQUEST with correct CSV links for both GB and XI authorities " +
      "if an invalid payload sent " +
      "and authorities-notification-panel-enabled feature flag is true" in new Setup {

        val authCsvFiles: Seq[StandingAuthorityFile] =
          Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(authCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        running(newApp) {
          val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> "ERROR")

          val result = route(newApp, request).value
          val html   = Jsoup.parse(contentAsString(result))

          status(result) shouldBe BAD_REQUEST

          html.getElementById("gb-csv-authority-link").html()       shouldBe
            messages(newApp)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href") shouldBe gbStanAuthFile154Url

          html.getElementById("xi-csv-authority-link").html()       shouldBe
            messages(newApp)("cf.authorities.notification-panel.a.xi-authority")
          html.getElementById("xi-csv-authority-link").attr("href") shouldBe xiStanAuthFile154Url
        }
      }

    "return BAD_REQUEST with correct CSV links for both GB and XI authorities " +
      "if an invalid payload sent " +
      "and authorities-notification-panel-enabled feature flag is false" in new Setup {
        val authCsvFiles: Seq[StandingAuthorityFile] =
          Seq(gbStandingAuth1, gbStandingAuth2, xiStandingAuth1, xiStandingAuth2)

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(authCsvFiles))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> false
          )
          .build()

        running(newApp) {
          val request = fakeRequest(POST, routes.AuthorizedToViewController.onSubmit().url)
            .withFormUrlEncodedBody("value" -> "ERROR")

          val result = route(newApp, request).value
          val html   = Jsoup.parse(contentAsString(result))

          status(result) shouldBe BAD_REQUEST

          Option(html.getElementById("gb-csv-authority-link")) shouldBe None
          Option(html.getElementById("xi-csv-authority-link")) shouldBe None
        }
      }
    "return SEE_OTHER and redirect to /authorities-search-results:searchQuery " +
      "and /authorities-search-results:searchQury should return BAD_REQUEST " +
      "with correct error msg when agent is not registered for his own XI EORI " +
      "and search authority using trader's XI EORI" in new Setup {

        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(Seq()))
        when(mockDataStoreService.getXiEori(any[HeaderCarrier])).thenReturn(Future.successful(None))

        validateRedirectToOnSearchAndThen(xiEORI, app) { (result, html) =>
          status(result) shouldBe BAD_REQUEST
          html.text().contains(messages(app)("cf.search.authorities.error.register-xi-eori"))
        }
      }

    "return Internal Server Error and go to view search no result page when there are errors from the API while" +
      "retrieving authorities for GB and XI EORI for EORI" in new Setup {
        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(Future.successful(Left(SearchError)))
          .thenReturn(Future.successful(Left(SearchError)))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        validateRedirectToOnSearchAndThen(gbEORI, app) { (result, _) =>
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }

    "return Internal Server Error when there are errors from the API while" +
      "retrieving authorities for GB and XI EORI for input account number" in new Setup {
        when(mockApiService.searchAuthorities(any, any)(any))
          .thenReturn(Future.successful(Left(SearchError)))
          .thenReturn(Future.successful(Left(SearchError)))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        validateRedirectToOnSearchAndThen("1000000", app) { (result, _) =>
          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }

    "Display error message if searching your own EORI number" +
      "when authorities-notification-panel-enabled feature flag is true" in new Setup {
        val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)
        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        validateRedirectToOnSearchAndThen(newUser().eori, newApp) { (result, html) =>
          status(result)                                                 shouldBe BAD_REQUEST
          html.getElementById("gb-csv-authority-link").html()            shouldBe
            messages(newApp)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href")      shouldBe gbStanAuthFile154Url
          html.text().contains("You cannot search your own EORI number") shouldBe true
        }
      }

    "Display error message if searching your own XI EORI number " +
      "when authorities-notification-panel-enabled feature flag is true" in new Setup {
        val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)
        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Some("XI123456789912")))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> true
          )
          .build()

        validateRedirectToOnSearchAndThen("XI123456789912", newApp) { (result, html) =>
          status(result)                                                 shouldBe BAD_REQUEST
          html.getElementById("gb-csv-authority-link").html()            shouldBe
            messages(newApp)("cf.authorities.notification-panel.a.gb-authority")
          html.getElementById("gb-csv-authority-link").attr("href")      shouldBe gbStanAuthFile154Url
          html.text().contains("You cannot search your own EORI number") shouldBe true
        }
      }

    "Display error message if searching your own XI EORI number " +
      "when authorities-notification-panel-enabled feature flag is false" in new Setup {
        val gbAuthCsvFiles: Seq[StandingAuthorityFile] = Seq(gbStandingAuth1, gbStandingAuth2)
        when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any)).thenReturn(Future.successful(gbAuthCsvFiles))

        when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Some("XI123456789912")))

        val newApp: Application = application()
          .overrides(
            inject.bind[ApiService].toInstance(mockApiService),
            inject.bind[DataStoreService].toInstance(mockDataStoreService),
            inject.bind[SdesConnector].toInstance(mockSdesConnector)
          )
          .configure(
            "features.new-agent-view-enabled"                 -> false,
            "features.authorities-notification-panel-enabled" -> false
          )
          .build()

        validateRedirectToOnSearchAndThen("XI123456789912", newApp) { (result, html) =>
          status(result)                                                 shouldBe BAD_REQUEST
          Option(html.getElementById("gb-csv-authority-link"))           shouldBe None
          html.text().contains("You cannot search your own EORI number") shouldBe true
        }
      }

    "Display error message if searching your own account number" in new Setup {
      when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))

      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
        .thenReturn(Future.successful(cdsAccounts))

      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().xiEori.get))(any))
        .thenReturn(Future.successful(xiCdsAccounts))

      validateRedirectToOnSearchAndThen(accounts.map(_.number).head, app) { (result, html) =>
        status(result)                                                    shouldBe BAD_REQUEST
        html.text().contains("You cannot search your own account number") shouldBe true
      }
    }

    "Display error message if searching your own XI DD account number" in new Setup {
      when(mockDataStoreService.getXiEori(any))
        .thenReturn(Future.successful(Some("XI123456789012")))

      when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
        .thenReturn(Future.successful(cdsAccounts))

      when(mockApiService.getAccounts(ArgumentMatchers.eq("XI123456789012"))(any))
        .thenReturn(Future.successful(xiCdsAccounts))

      validateRedirectToOnSearchAndThen(accounts.map(_.number).head, app) { (result, html) =>
        status(result)                                                    shouldBe BAD_REQUEST
        html.text().contains("You cannot search your own account number") shouldBe true
      }
    }
  }

  "The header section" should {
    "have a back to accounts link on top" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value
        val html    = Jsoup.parse(contentAsString(result))

        html.containsLinkWithText("/customs/payment-records", "link-back")
      }
    }

    "have a heading field" in new Setup {

      when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email(emailId))))

      running(app) {
        val request = fakeRequest(GET, routes.AuthorizedToViewController.onPageLoad().url)
        val result  = route(app, request).value
        val html    = Jsoup.parse(contentAsString(result))

        html.getElementById("h1") should not be emptyString
      }
    }
  }

  trait Setup {

    val dd1: DutyDefermentAccount = DutyDefermentAccount(
      "1231231231",
      newUser().eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_200)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(BALANCE_50)),
        Some(BigDecimal(BALANCE_20))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd2: DutyDefermentAccount = DutyDefermentAccount(
      "7567567567",
      newUser().eori,
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(BALANCE_200)), Some(BigDecimal(BALANCE_100)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd3: DutyDefermentAccount = DutyDefermentAccount(
      "7897897897",
      "testEori10",
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_200)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(BALANCE_50)),
        Some(BigDecimal(BALANCE_20))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val dd4: DutyDefermentAccount = DutyDefermentAccount(
      "1112223334",
      "testEori11",
      isNiAccount = false,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(Some(BigDecimal(BALANCE_200)), Some(BigDecimal(BALANCE_100)), None, None),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val xiDd: DutyDefermentAccount = DutyDefermentAccount(
      "1231231000",
      newUser().xiEori.get,
      isNiAccount = true,
      AccountStatusOpen,
      DefermentAccountAvailable,
      DutyDefermentBalance(
        Some(BigDecimal(BALANCE_200)),
        Some(BigDecimal(BALANCE_100)),
        Some(BigDecimal(BALANCE_50)),
        Some(BigDecimal(BALANCE_20))
      ),
      viewBalanceIsGranted = true,
      isIsleOfMan = false
    )

    val cashAccount1: CashAccount = CashAccount(
      "1000000",
      "testEori10",
      AccountStatusOpen,
      DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(BALANCE_100)))
    )

    val cashAccount2: CashAccount =
      CashAccount("2000000", "testEori11", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(None))

    val ggAccount1: GeneralGuaranteeAccount = GeneralGuaranteeAccount(
      "1234444",
      "testEori12",
      AccountStatusOpen,
      DefermentAccountAvailable,
      Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_500), BigDecimal(BALANCE_300)))
    )

    val ggAccount2: GeneralGuaranteeAccount =
      GeneralGuaranteeAccount("2235555", "testEori13", AccountStatusOpen, DefermentAccountAvailable, None)

    val accounts: List[CDSAccount]             = List(dd1, dd2, dd3, dd4, cashAccount1, cashAccount2, ggAccount1, ggAccount2)
    val xiAccounts: List[DutyDefermentAccount] = List(xiDd)

    val cdsAccounts: CDSAccounts      = CDSAccounts(newUser().eori, None, accounts)
    val xiCdsAccounts: CDSAccounts    = CDSAccounts(newUser().xiEori.get, None, xiAccounts)
    val emptyCdsAccounts: CDSAccounts = CDSAccounts(newUser().eori, None, List())

    val gbEORI = "GB123456789012"
    val xiEORI = "XI123456789012"

    val gbStanAuthFile153Url = "https://test.co.uk/GB123456789012/SA_000000000153_csv.csv"
    val gbStanAuthFile154Url = "https://test.co.uk/GB123456789012/SA_000000000154_csv.csv"
    val xiStanAuthFile153Url = "https://test.co.uk/XI123456789012/SA_000000000153_XI_csv.csv"
    val xiStanAuthFile154Url = "https://test.co.uk/XI123456789012/SA_000000000154_XI_csv.csv"

    val standAuthMetadata: StandingAuthorityMetadata =
      StandingAuthorityMetadata(YEAR_2022, MONTH_6, DAY_1, Csv, StandingAuthority)

    val gbStandingAuth1: StandingAuthorityFile =
      StandingAuthorityFile("SA_000000000153_csv.csv", gbStanAuthFile153Url, FILE_SIZE_500, standAuthMetadata, gbEORI)
    val gbStandingAuth2: StandingAuthorityFile =
      StandingAuthorityFile("SA_000000000154_csv.csv", gbStanAuthFile154Url, FILE_SIZE_500, standAuthMetadata, gbEORI)

    val xiStandingAuth1: StandingAuthorityFile = StandingAuthorityFile(
      "SA_XI_000000000153_csv.csv",
      xiStanAuthFile153Url,
      FILE_SIZE_500,
      standAuthMetadata,
      xiEORI
    )
    val xiStandingAuth2: StandingAuthorityFile = StandingAuthorityFile(
      "SA_XI_000000000154_XI_csv.csv",
      xiStanAuthFile154Url,
      FILE_SIZE_500,
      standAuthMetadata,
      xiEORI
    )

    val emailId = "test@test.com"

    val mockApiService: ApiService             = mock[ApiService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]
    val mockSdesConnector: SdesConnector       = mock[SdesConnector]

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(any))
      .thenReturn(Future.successful(cdsAccounts))
    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().xiEori.get))(any))
      .thenReturn(Future.successful(xiCdsAccounts))
    when(mockApiService.getAccounts(ArgumentMatchers.anyString())(any))
      .thenReturn(Future.successful(emptyCdsAccounts))
    when(mockSdesConnector.getAuthoritiesCsvFiles(any)(any))
      .thenReturn(Future.successful(Seq.empty))

    val app: Application = application()
      .overrides(
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[SdesConnector].toInstance(mockSdesConnector)
      )
      .configure("features.new-agent-view-enabled" -> false)
      .build()
  }
}

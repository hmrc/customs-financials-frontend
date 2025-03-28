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

import connectors.CustomsFinancialsSessionCacheConnector
import domain.{
  AccountStatusOpen, CDSAccount, CDSAccounts, CDSCashBalance, CashAccount, DefermentAccountAvailable,
  DutyDefermentAccount, DutyDefermentBalance, GeneralGuaranteeAccount, GeneralGuaranteeBalance, XiEoriAddressInformation
}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when

import play.api.http.Status
import play.api.i18n.Messages
import play.api.{Application, inject}
import play.api.test.Helpers
import play.api.test.Helpers._
import services.{ApiService, DataStoreService, NotificationService, XiEoriInformationReponse}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HttpResponse
import utils.SpecBase
import utils.TestData.{FILE_SIZE_888, LENGTH_8}

import scala.jdk.CollectionConverters._
import scala.concurrent.Future
import scala.util.Random
import utils.MustMatchers

class HomeControllerCardSpec extends SpecBase with MustMatchers {

  "CustomsFinancialsHomeController" should {

    "the landing page" should {
      "show the duty deferment account cards" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.getElementsByClass("duty-deferment-account").isEmpty mustBe false
        }
      }

      "show the cash account card" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.getElementsByClass("cash-account").isEmpty mustBe false
        }
      }

      "show the guarantee account card" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.getElementsByClass("guarantee-account").isEmpty mustBe false
        }
      }

      "show the import vat card" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText("http://localhost:9398/customs/documents/import-vat", "View certificates")
        }
      }

      "show the securities card" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/adjustments",
            "View notification of adjustment statements"
          ) mustBe true
        }
      }

      "show the postponed vat card" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/postponed-vat?location=CDS",
            "View postponed import VAT statements"
          ) mustBe true
        }
      }
    }

    "display vat card" should {
      "work with only actual eori statements and no historic Eoris in the eori store" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/import-vat",
            "View import VAT certificates (C79)"
          ) mustBe true
        }
      }

      "work with actual and historic eori statements" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/import-vat",
            "View import VAT certificates (C79)"
          ) mustBe true
        }
      }
    }

    "display securities card" should {
      "work with only actual eori statements and no historic Eoris in the eori store" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/adjustments",
            "View notification of adjustment statements"
          ) mustBe true
        }
      }

      "work with only eori statements and historic Eoris in the store, but not historic statements" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/adjustments",
            "View notification of adjustment statements"
          ) mustBe true
        }
      }
    }

    "display postponed vat card" should {
      "work with only actual eori statements and no historic Eoris in the eori store" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/postponed-vat?location=CDS",
            "View postponed import VAT statements"
          ) mustBe true
        }
      }

      "work with only eori statements and historic Eoris in the store, but not historic statements" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
          val result  = route(app, request).value
          val html    = Jsoup.parse(contentAsString(result))

          html.containsLinkWithText(
            "http://localhost:9398/customs/documents/postponed-vat?location=CDS",
            "View postponed import VAT statements"
          ) mustBe true
        }
      }
    }

    "show the historic eori duty deferment cards" in {

      val currentEoriDDAccount = DutyDefermentAccount(
        "678910",
        "11111",
        isNiAccount = false,
        AccountStatusOpen,
        DefermentAccountAvailable,
        DutyDefermentBalance(Some(110.00), Some(210.00), Some(31.00), Some(41.00)),
        viewBalanceIsGranted = true,
        isIsleOfMan = false
      )

      val historicEoriDDAccount = DutyDefermentAccount(
        "12345",
        "22222",
        isNiAccount = false,
        AccountStatusOpen,
        DefermentAccountAvailable,
        DutyDefermentBalance(Some(100.00), Some(200.00), Some(30.00), Some(40.00)),
        viewBalanceIsGranted = true,
        isIsleOfMan = false
      )

      val add = XiEoriAddressInformation(emptyString, Some(emptyString), None, None, Some(emptyString))
      val xi  = XiEoriInformationReponse("Some XiEori", "yes", add)

      val mockAccounts              = mock[CDSAccounts]
      val mockApiService            = mock[ApiService]
      val mockNotificationService   = mock[NotificationService]
      val mockDataStoreService      = mock[DataStoreService]
      val accounts                  = mock[CDSAccounts]
      val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

      when(accounts.myAccounts).thenReturn(List(currentEoriDDAccount, historicEoriDDAccount))
      when(accounts.accounts).thenReturn(List(currentEoriDDAccount, historicEoriDDAccount))
      when(accounts.isAgent).thenReturn(false)
      when(accounts.isNiAccount).thenReturn(Some(false))
      when(mockApiService.getAccounts(any)(any)).thenReturn(Future.successful(accounts))
      when(mockApiService.getAccounts(any)(any)).thenReturn(Future.successful(accounts))
      when(mockNotificationService.fetchNotifications(any)).thenReturn(Future.successful(List()))
      when(mockNotificationService.fetchNotifications(any)).thenReturn(Future.successful(List()))

      when(mockDataStoreService.getEmail(any))
        .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))

      when(mockSessionCacheConnector.storeSession(any, any)(any))
        .thenReturn(Future.successful(HttpResponse(OK, emptyString)))

      when(mockDataStoreService.getCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Company Name")))

      when(mockDataStoreService.getOwnCompanyName(any))
        .thenReturn(Future.successful(Some("Test Own Company Name")))

      when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Some(xi.xiEori)))

      val app = application()
        .overrides(
          inject.bind[CDSAccounts].toInstance(mockAccounts),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[NotificationService].toInstance(mockNotificationService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService),
          inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
        )
        .build()

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result  = route(app, request).value
        val html    = Jsoup.parse(contentAsString(result))

        html
          .getElementsByClass("duty-deferment-account")
          .tagName("h3")
          .asScala
          .exists(_.text.contains("Account: 678910")) mustBe true

        html
          .getElementsByClass("duty-deferment-account")
          .tagName("h3")
          .asScala
          .exists(_.text.contains("Account: 12345")) mustBe true
      }
    }
  }

  trait Setup {

    implicit val messages: Messages = Helpers.stubMessages()

    val eoriNumber = "testEori1"

    val someAccounts: List[CDSAccount] = {
      val someGuaranteeAccountNumber    = "1234567"
      val someGuaranteeLimit            = 56789
      val someAvailableGuaranteeBalance = 98765.43
      val someGuaranteeAccount          = GeneralGuaranteeAccount(
        someGuaranteeAccountNumber,
        eoriNumber,
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(someGuaranteeLimit), BigDecimal(someAvailableGuaranteeBalance)))
      )
      val someCashAccount               = CashAccount(
        "1000001",
        eoriNumber,
        AccountStatusOpen,
        DefermentAccountAvailable,
        CDSCashBalance(Some(BigDecimal(FILE_SIZE_888)))
      )

      val ownAccounts = (1 until 3).map { _ =>
        DutyDefermentAccount(
          Random.alphanumeric.take(LENGTH_8).mkString,
          eoriNumber,
          isNiAccount = false,
          AccountStatusOpen,
          DefermentAccountAvailable,
          DutyDefermentBalance(
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble))
          ),
          viewBalanceIsGranted = true,
          isIsleOfMan = false
        )
      }.toList

      val authorizedToViewAccounts = (1 until 2).map { _ =>
        DutyDefermentAccount(
          Random.alphanumeric.take(LENGTH_8).mkString,
          Random.alphanumeric.take(LENGTH_8).mkString,
          isNiAccount = false,
          AccountStatusOpen,
          DefermentAccountAvailable,
          DutyDefermentBalance(
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble))
          ),
          viewBalanceIsGranted = true,
          isIsleOfMan = false
        )
      }.toList

      ownAccounts ++ authorizedToViewAccounts ++ List(someGuaranteeAccount) ++ List(someCashAccount)
    }

    val mockAccounts: CDSAccounts                                         = mock[CDSAccounts]
    val mockApiService: ApiService                                        = mock[ApiService]
    val mockNotificationService: NotificationService                      = mock[NotificationService]
    val mockDataStoreService: DataStoreService                            = mock[DataStoreService]
    val mockSessionCacheConnector: CustomsFinancialsSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    when(mockNotificationService.fetchNotifications(any)).thenReturn(Future.successful(List.empty))

    when(mockApiService.getAccounts(any)(any))
      .thenReturn(Future.successful(mockAccounts))

    val add: XiEoriAddressInformation =
      XiEoriAddressInformation(emptyString, Some(emptyString), None, None, Some(emptyString))

    val xi: XiEoriInformationReponse = XiEoriInformationReponse("Some XiEori", "yes", add)

    when(mockAccounts.myAccounts).thenReturn(someAccounts)
    when(mockAccounts.accounts).thenReturn(someAccounts)
    when(mockAccounts.isAgent).thenReturn(false)
    when(mockAccounts.isNiAccount).thenReturn(Some(false))
    when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
    when(mockDataStoreService.getCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Company Name")))
    when(mockDataStoreService.getOwnCompanyName(any)).thenReturn(Future.successful(Some("Test Own Company Name")))
    when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(Some(xi.xiEori)))

    when(mockSessionCacheConnector.storeSession(any, any)(any))
      .thenReturn(Future.successful(HttpResponse(Status.OK, emptyString)))

    val app: Application = application()
      .overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      )
      .build()
  }
}

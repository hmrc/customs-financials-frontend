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

import config.AppConfig
import connectors.{CustomsFinancialsSessionCacheConnector, CustomsManageAuthoritiesConnector}
import domain.FileRole.{
  C79Certificate, DutyDefermentStatement, PostponedVATAmendedStatement, PostponedVATStatement,
  SecurityStatement, StandingAuthority
}
import domain.{
  AccountStatusOpen, CDSAccount, CDSAccounts, CDSCashBalance, CashAccount, DefermentAccountAvailable,
  DutyDefermentAccount, DutyDefermentBalance, GeneralGuaranteeAccount, GeneralGuaranteeBalance, XiEoriAddressInformation
}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when

import play.api.http.Status
import play.api.i18n.Messages
import play.api.mvc.Results.Ok
import play.api.test.Helpers
import play.api.test.Helpers._
import play.api.{Application, inject}
import services._
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.{GatewayTimeoutException, HttpResponse, InternalServerException, SessionId}
import utils.SpecBase
import utils.TestData.{BALANCE_888, LENGTH_8}
import viewmodels.FinancialsHomeModel

import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.util.Random

class HomeControllerSpec extends SpecBase {

  "CustomsFinancialsHomeController" should {
    val eori1 = "EORI0123"
    val eori2 = "EORI3210"
    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    "FinancialsHomeModel generate links for each Duty Deferment Account" in {
      def randomFloat: Float = Random.nextFloat()

      def randomBigDecimal: BigDecimal = BigDecimal(randomFloat.toString)

      val sessionId = SessionId("session")
      val eoriNumber = newUser(Seq.empty).eori
      val companyName = Some("Company Name 1")

      val cdsAccounts = Seq(
        CDSAccounts(eoriNumber, None, Seq(DutyDefermentAccount(dan1, eori1, isNiAccount = false,
          AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal),
            Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)),
          viewBalanceIsGranted = true, isIsleOfMan = false))),

        CDSAccounts(eoriNumber, None, Seq(DutyDefermentAccount(dan2, eori2, isNiAccount = false,
          AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal),
            Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)),
          viewBalanceIsGranted = true, isIsleOfMan = false)))
      )

      val newApp = application().build()
      val appConfig = newApp.injector.instanceOf[AppConfig]

      running(newApp) {
        val controller = newApp.injector.instanceOf[CustomsFinancialsHomeController]
        val accountLinks = controller.createAccountLinks(sessionId, cdsAccounts)

        val model = FinancialsHomeModel(eoriNumber,
          companyName, cdsAccounts, notificationMessageKeys = List(), accountLinks, None)

        model.dutyDefermentAccountDetailsLinks()(appConfig)((eori1, dan1))
        model.dutyDefermentAccountDetailsLinks()(appConfig)((eori2, dan2))
      }
    }
  }

  "the landing page" should {
    "display 'Your customs financial accounts' and EORI" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByTag("h1").text mustBe "Your import duties and VAT accounts"
      }
    }
  }

  "have the Import VAT section heading" in new Setup {
    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.getElementsContainingText("Import VAT certificates (C79)").isEmpty mustBe false
    }
  }

  "not show notification even when there is new C97Statement available" in new Setup {
    val notifications: List[Notification] = List(Notification(C79Certificate, isRequested = false))

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
      Future.successful(notifications))

    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByClass("notification-panel").isEmpty mustBe true
    }
  }

  "show notification when there is new C97Statement available" in new Setup {
    val notifications: List[Notification] = List(Notification(C79Certificate, isRequested = false))

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
      Future.successful(notifications))

    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.containsElementById("notification-panel")
    }
  }

  "with security statements available" should {
    "show the Import adjustments section heading" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByTag("h2")
          .asScala.exists(_.text == "Notification of adjustment statements") mustBe true
      }
    }
  }

  "show notification when there is new Securities Statement available" in new Setup {

    val notifications: List[Notification] = List(Notification(SecurityStatement, isRequested = false))

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
      Future.successful(notifications))

    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.containsElementById("notification-panel")
    }
  }

  "with no security statements available" should {
    "omit the Import adjustments section heading" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByTag("h2").asScala.exists(_.text.contains("Import adjustments")) mustBe false
      }
    }
  }

  "show the Import VAT section heading" in new Setup {
    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.getElementsByTag("h2")
        .asScala.exists(_.text.contains("Postponed import VAT statements")) mustBe true
    }
  }

  "show notification when there is new Postponed VAT Statement available" in new Setup {
    val notifications: List[Notification] = List(Notification(PostponedVATStatement, isRequested = false))

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
      Future.successful(notifications))

    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))
      html.containsElementById("notification-panel")
    }
  }

  "show notification when there is new Standing authorities csv file available" in new Setup {
    val notifications: List[Notification] = List(Notification(StandingAuthority, isRequested = false))

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
      Future.successful(notifications))

    running(app) {
      val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
      val result = route(app, request).value
      val html = Jsoup.parse(contentAsString(result))

      html.containsElementById("notification-panel")
    }
  }

  "partial landing page" should {
    "show error message as heading text" in {

      val mockAccounts = mock[CDSAccounts]
      val mockApiService = mock[ApiService]
      val mockNotificationService = mock[NotificationService]

      val app = application().overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService)
      ).build()

      val eoriNumber = newUser(Seq.empty).eori

      when(mockNotificationService.fetchNotifications(
        eqTo(eoriNumber))(any)).thenReturn(Future.successful(List.empty))

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.pageWithoutAccounts.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByClass("govuk-heading-xl")
          .text mustBe "Sorry, some parts of the service are unavailable at the moment"
      }
    }

    "show notifications for only C79 PVAT & Securities statements" in {

      val mockAccounts = mock[CDSAccounts]
      val mockApiService = mock[ApiService]
      val mockNotificationService = mock[NotificationService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()

      val notifications = List(Notification(C79Certificate, isRequested = false),
        Notification(PostponedVATStatement, isRequested = false),
        Notification(SecurityStatement, isRequested = false),
        Notification(DutyDefermentStatement, isRequested = true),
        Notification(DutyDefermentStatement, isRequested = false),
        Notification(StandingAuthority, isRequested = false))

      val eoriNumber = newUser(Seq.empty).eori

      when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(
        Future.successful(notifications))

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.pageWithoutAccounts.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        val notificationsText = html.select("#notification-panel p").asScala.map(_.text()).toList

        notificationsText mustBe List("You have a new import adjustments statement",
          "You have a new import VAT (C79) certificate",
          "You have a new postponed import VAT statement")
      }
    }

    "show multiple notification message when multiple of the same notifications are present" in {
      val mockAccounts = mock[CDSAccounts]
      val mockApiService = mock[ApiService]
      val mockNotificationService = mock[NotificationService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()

      val notifications = List(
        Notification(C79Certificate, isRequested = false),
        Notification(C79Certificate, isRequested = false),
        Notification(C79Certificate, isRequested = true),
        Notification(C79Certificate, isRequested = true),
        Notification(PostponedVATStatement, isRequested = false),
        Notification(PostponedVATStatement, isRequested = false),
        Notification(SecurityStatement, isRequested = false),
        Notification(SecurityStatement, isRequested = false),
        Notification(SecurityStatement, isRequested = true),
        Notification(SecurityStatement, isRequested = true))

      when(mockNotificationService.fetchNotifications(any)(any)).thenReturn(Future.successful(notifications))

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.pageWithoutAccounts.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        val notificationsText = html.select("#notification-panel p").asScala.map(_.text()).toList

        notificationsText mustBe List(
          "Your requested import VAT certificates (C79) are available to view",
          "Your requested import adjustments statements are available to view",
          "You have new import adjustment statements",
          "You have new import VAT certificates (C79)",
          "You have new postponed import VAT statements"
        )
      }
    }
  }

  "redirect to partial landing page" when {
    "failed to get all accounts" in {
      val mockAccounts = mock[CDSAccounts]
      val mockApiService = mock[ApiService]
      val mockNotificationService = mock[NotificationService]
      val mockDataStoreService = mock[DataStoreService]
      val mockManageAuthoritiesConnector = mock[CustomsManageAuthoritiesConnector]

      val app = application().overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[CustomsManageAuthoritiesConnector].toInstance(mockManageAuthoritiesConnector)
      ).build()

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(
        Future.successful(Right(Email("last.man@standing.co.uk"))))

      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      when(mockApiService.getAccounts(any)(any)).thenReturn(
        Future.failed(new InternalServerException("SPS is Down")))

      when(mockManageAuthoritiesConnector.fetchAndSaveAccountAuthoritiesInCache(any)(any))
        .thenReturn(Future.successful(Ok))

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.CustomsFinancialsHomeController.pageWithoutAccounts.url
      }
    }
  }

  "get message keys" should {
    "find supported sdes notification" in {
      val app = application().overrides().build()
      val controller = app.injector.instanceOf[CustomsFinancialsHomeController]

      running(app) {
        val notifications = List(Notification(C79Certificate, isRequested = false))
        val actualResult: Seq[String] = controller.getNotificationMessageKeys(notifications)

        actualResult mustBe Seq("c79")
      }
    }

    "ignore the unsupported sdes notification" in {
      val app = application().overrides().build()
      val controller = app.injector.instanceOf[CustomsFinancialsHomeController]

      running(app) {
        val notifications = List(Notification(PostponedVATAmendedStatement, isRequested = true))
        val actualResult: Seq[String] = controller.getNotificationMessageKeys(notifications)

        actualResult mustBe Seq()
      }
    }
  }

  "redirect to account unavailable page " when {
    "failed to get all accounts" in {
      val mockAccounts = mock[CDSAccounts]
      val mockApiService = mock[ApiService]
      val mockNotificationService = mock[NotificationService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()

      when(mockDataStoreService.getEmail(any)(any)).thenReturn(Future.successful(

        Right(Email("last.man@standing.co.uk"))))
      when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(None))

      when(mockApiService.getAccounts(any)(any)).thenReturn(Future.failed(
        new GatewayTimeoutException("Request Timeout")))

      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url)
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.CustomsFinancialsHomeController.showAccountUnavailable.url
      }
    }
  }

  trait Setup {

    implicit val messages: Messages = Helpers.stubMessages()

    val eoriNumber = "testEori1"

    val someAccounts: List[CDSAccount] = {
      val someGuaranteeAccountNumber = "1234567"
      val someGuaranteeLimit = 56789
      val someAvailableGuaranteeBalance = 98765.43
      val someGuaranteeAccount = GeneralGuaranteeAccount(
        someGuaranteeAccountNumber,
        eoriNumber,
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(someGuaranteeLimit), BigDecimal(someAvailableGuaranteeBalance)))
      )

      val someCashAccount = CashAccount("1000001", eoriNumber, AccountStatusOpen,
        DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(BALANCE_888))))

      val ownAccounts = (1 until 3).map { _ =>
        DutyDefermentAccount(
          Random.alphanumeric.take(LENGTH_8).mkString,
          eoriNumber, isNiAccount = false,
          AccountStatusOpen,
          DefermentAccountAvailable,
          DutyDefermentBalance(
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble)),
            Some(BigDecimal(Random.nextFloat().toDouble))),
          viewBalanceIsGranted = true, isIsleOfMan = false)
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
            Some(BigDecimal(Random.nextFloat().toDouble)))
          , viewBalanceIsGranted = true, isIsleOfMan = false)
      }.toList

      ownAccounts ++ authorizedToViewAccounts ++ List(someGuaranteeAccount) ++ List(someCashAccount)
    }

    val add: XiEoriAddressInformation =
      XiEoriAddressInformation(emptyString, Some(emptyString), None, None, Some(emptyString))

    val xi: XiEoriInformationReponse = XiEoriInformationReponse("Some XiEori", "yes", add)

    val mockAccounts: CDSAccounts = mock[CDSAccounts]
    val mockApiService: ApiService = mock[ApiService]
    val mockNotificationService: NotificationService = mock[NotificationService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]
    val mockSessionCacheConnector: CustomsFinancialsSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]
    val mockManageAuthoritiesConnector: CustomsManageAuthoritiesConnector = mock[CustomsManageAuthoritiesConnector]

    when(mockNotificationService.fetchNotifications(eqTo(eoriNumber))(any)).thenReturn(Future.successful(List.empty))
    when(mockApiService.getAccounts(any)(any)).thenReturn(Future.successful(mockAccounts))
    when(mockAccounts.myAccounts).thenReturn(someAccounts)
    when(mockAccounts.accounts).thenReturn(someAccounts)
    when(mockAccounts.isAgent).thenReturn(false)
    when(mockAccounts.isNiAccount).thenReturn(Some(false))

    when(mockDataStoreService.getEmail(any)(any)).thenReturn(
      Future.successful(Right(Email("last.man@standing.co.uk"))))

    when(mockDataStoreService.getCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Company Name")))
    when(mockDataStoreService.getOwnCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Own Company Name")))

    when(mockSessionCacheConnector.storeSession(any, any)(any)).thenReturn(
      Future.successful(HttpResponse(Status.OK, emptyString)))

    when(mockDataStoreService.getXiEori(any)(any)).thenReturn(Future.successful(Some(xi.xiEori)))
    when(mockManageAuthoritiesConnector.fetchAndSaveAccountAuthoritiesInCache(any)(any))
      .thenReturn(Future.successful(Ok))

    val app: Application = application().overrides(
      inject.bind[CDSAccounts].toInstance(mockAccounts),
      inject.bind[ApiService].toInstance(mockApiService),
      inject.bind[NotificationService].toInstance(mockNotificationService),
      inject.bind[DataStoreService].toInstance(mockDataStoreService),
      inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector),
      inject.bind[CustomsManageAuthoritiesConnector].toInstance(mockManageAuthoritiesConnector)
    ).build()
  }
}

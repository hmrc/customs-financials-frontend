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

package services


import domain.FileRole.{C79Certificate, DutyDefermentStatement, PostponedVATStatement, SecurityStatement}
import domain._
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, _}
import utils.SpecBase
import domain.{AccountResponse, AccountsAndBalancesResponseContainer, Limits, CdsCashAccountResponse => CA, DefermentBalancesResponse => Bal, DutyDefermentAccountResponse => DDA, GeneralGuaranteeAccountResponse => GGA}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api
import play.api.inject
import play.api.mvc.Results.Status

import scala.concurrent.Future

//noinspection TypeAnnotation
class ApiServiceSpec extends SpecBase
  with FutureAwaits with DefaultAwaitTimeout with ScalaFutures {

  "ApiService" should {
    "getAccounts" should {
      "return all accounts available to the given EORI from the API service" in new Setup() {
        when[Future[AccountsAndBalancesResponseContainer]](mockHttpClient.POST(any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(traderAccounts))

        val service = app.injector.instanceOf[ApiService]
        running(app) {
          val result = await(service.getAccounts(traderEori))
          result must be(traderAccounts.toCdsAccounts(traderEori))
        }
      }

      "log response time metric" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val mockHttpClient = mock[HttpClient]
        val mockMetricsReporterService = mock[MetricsReporterService]
        val appTest = application().overrides(
          inject.bind[HttpClient].toInstance(mockHttpClient),
          inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()

        val traderEori = "12345678"
        val guaranteeAccount = GGA(AccountResponse("G123456", "", traderEori, None, None, viewBalanceIsGranted = false, isleOfManFlag = None), Some("1000000"), Some("200000"))
        val dd1 = DDA(AccountResponse("1231231231", "", traderEori, None, None, false, None), Some(Limits("200", "100")), Some(Bal("50", "20")))
        val dd2 = DDA(AccountResponse("7567567567", "", traderEori, None, None, false, None), Some(Limits("200", "100")), None)
        val cashAccountNumber = "987654"
        val cashAccount = CA(AccountResponse(cashAccountNumber, "", traderEori, None, None, false, None), Some("999.99"))

        val accounts = AccountsAndBalancesResponseContainer(
          domain.AccountsAndBalancesResponse(
            Some(domain.AccountResponseCommon("", Some(""), "", None)),
            domain.AccountResponseDetail(
              Some(""),
              None,
              Some(Seq(dd1, dd2)),
              Some(Seq(guaranteeAccount)),
              Some(Seq(cashAccount))
            )
          )
        )

        when[Future[Seq[DutyDefermentAccount]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
          .thenReturn(Future.successful(Seq(dd1.toDomain())))
        when[Future[AccountsAndBalancesResponseContainer]](mockHttpClient.POST(any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(accounts))
        running(appTest) {
          val service = appTest.injector.instanceOf[ApiService]
          await(service.getAccounts(traderEori))
          verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("customs-financials-api.get.accounts"))(any)(any)
        }
      }


      "return all accounts available for the given EORI from the API service for Acc27" in new Setup() {
        when[Future[AccountsAndBalancesResponseContainer]](mockHttpClient.POST(any, any, any)(any, any, any, any))
          .thenReturn(Future.successful(traderAccountsWithNoCommonResponse))
        val service = app.injector.instanceOf[ApiService]
        running(app) {
          val result = await(service.getAccounts(traderEori))
          result must be(traderAccountsWithNoCommonResponse.toCdsAccounts(traderEori))
        }
      }
    }
  }

  "getNotifications" should {
    "return a Notifications for the given EORI" in new Setup() {
      val notification = List(DocumentAttributes(traderEori, C79Certificate, "new file", 1000, Map.empty))
      val notifications = SdesNotificationsForEori(traderEori, notification)
      when[Future[SdesNotificationsForEori]](mockHttpClient.GET(any, any, any)(any, any, any)).thenReturn(Future.successful(notifications))
      val service = app.injector.instanceOf[ApiService]
      running(app) {
        val result = await(service.getEnabledNotifications(traderEori))
        result must be(notification)
      }
    }

    "log response time metric" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val traderEori = "12345678"
      val mockMetricsReporterService = mock[MetricsReporterService]
      val notification = List(
        DocumentAttributes(traderEori, C79Certificate, "new file", 1000, Map.empty),
        DocumentAttributes(traderEori, DutyDefermentStatement, "new file", 1000, Map.empty),
        DocumentAttributes(traderEori, SecurityStatement, "new file", 1000, Map.empty),
        DocumentAttributes(traderEori, PostponedVATStatement, "new file", 1000, Map.empty))
      val notifications = SdesNotificationsForEori(traderEori, notification)
      val mockHttpClient = mock[HttpClient]
      val appTest = application().overrides(
        inject.bind[HttpClient].toInstance(mockHttpClient),
        inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
      ).build()
      when[Future[Seq[DocumentAttributes]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
        .thenReturn(Future.successful(notification))
      when[Future[SdesNotificationsForEori]](mockHttpClient.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(notifications))
      val service = appTest.injector.instanceOf[ApiService]
      running(appTest) {
        await(service.getEnabledNotifications(traderEori))
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("customs-financials-api.get.notifications"))(any)(any)
      }
    }

  }

  "deleteNotification" should {
    "send a delete notification request" in new Setup() {
      when(mockHttpClient.DELETE[HttpResponse](any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
      val service = app.injector.instanceOf[ApiService]
      running(app) {
        await(service.deleteNotification(traderEori, C79Certificate)(hc))
        verify(mockHttpClient).DELETE(any, any)(any, any, any)
      }
    }

    "log response time metric" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val traderEori = "12345678"
      val mockHttpClient = mock[HttpClient]
      val mockMetricsReporterService = mock[MetricsReporterService]
      val appTest = application().overrides(
        inject.bind[HttpClient].toInstance(mockHttpClient),
        inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
      ).build()
      when(mockHttpClient.DELETE[HttpResponse](any, any)(any, any, any))
        .thenReturn(Future.successful(HttpResponse.apply(OK, "")))
      when[Future[Boolean]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
        .thenReturn(Future.successful(true))
      val service = appTest.injector.instanceOf[ApiService]
      running(appTest) {
        await(service.deleteNotification(traderEori, C79Certificate))
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("customs-financials-api.delete.notification"))(any)(any)
      }
    }

  }


  trait Setup {
    val mockHttpClient = mock[HttpClient]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val traderEori = "12345678"
    val agentEori = "09876543"
    val guaranteeAccount = GGA(AccountResponse("G123456", "", traderEori, None, None, false, None), Some("1000000"), Some("200000"))
    val dd1 = DDA(AccountResponse("1231231231", "", traderEori, None, None, false, None), Some(Limits("200", "100")), Some(Bal("50", "20")))
    val dd2 = DDA(AccountResponse("7567567567", "", traderEori, None, None, false, None), Some(Limits("200", "100")), None)
    val cashAccountNumber = "987654"
    val cashAccount = CA(AccountResponse(cashAccountNumber, "", traderEori, None, None, false, None), Some("999.99"))
    val traderAccounts = AccountsAndBalancesResponseContainer(
      domain.AccountsAndBalancesResponse(
        Some(domain.AccountResponseCommon("", Some(""), "", None)),
        domain.AccountResponseDetail(
          Some(""),
          None,
          Some(Seq(dd1, dd2)),
          Some(Seq(guaranteeAccount)),
          Some(Seq(cashAccount))
        )
      )
    )
    val traderAccountsWithNoCommonResponse = AccountsAndBalancesResponseContainer(
      domain.AccountsAndBalancesResponse(
        None,
        domain.AccountResponseDetail(
          Some(""),
          None,
          Some(Seq(dd1, dd2)),
          Some(Seq(guaranteeAccount)),
          Some(Seq(cashAccount))
        )
      )
    )
    val app = application().overrides(
      inject.bind[HttpClient].toInstance(mockHttpClient)
    ).build()
  }
}
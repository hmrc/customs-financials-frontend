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

package services

import domain.FileRole.{C79Certificate, DutyDefermentStatement, PostponedVATStatement, SecurityStatement}
import domain.{AccountResponse, AccountsAndBalancesResponseContainer, Limits, CdsCashAccountResponse as CA,
  DefermentBalancesResponse as Bal, DutyDefermentAccountResponse as DDA, GeneralGuaranteeAccountResponse as GGA, *}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import play.api.{Application, inject}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.{HeaderCarrier,HttpReads, *}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.SpecBase
import utils.TestData.FILE_SIZE_1000
import org.scalatest.matchers.must.{Matchers => MustMatchers}

import scala.concurrent.Future

class ApiServiceSpec
  extends SpecBase
    with FutureAwaits
    with DefaultAwaitTimeout
    with ScalaFutures
    with MustMatchers {

  "ApiService" should {

    "getAccounts" should {
      "return all accounts available to the given EORI from the API service" in new Setup() {
        when(requestBuilder.withBody(any[AccountsAndBalancesRequestContainer])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[AccountsAndBalancesResponseContainer]], any[ExecutionContext]))
          .thenReturn(Future.successful(traderAccounts))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.getAccounts(traderEori))
          result must be(traderAccounts.toCdsAccounts(traderEori))
        }
      }

      "log response time metric" in {
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val mockHttpClient = mock[HttpClientV2]
        val requestBuilder = mock[RequestBuilder]
        val mockMetricsReporterService = mock[MetricsReporterService]

        val appTest = application().overrides(
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[RequestBuilder].toInstance(requestBuilder),
          inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()

        val traderEori = "12345678"
        val guaranteeAccount = GGA(AccountResponse("G123456", emptyString, traderEori, None, None,
          viewBalanceIsGranted = false), Some("1000000"), Some("200000"))

        val dd1 = DDA(AccountResponse("1231231231", emptyString, traderEori, None, None, viewBalanceIsGranted = false),
          Some(false), Some(false), Some(Limits("200", "100")), Some(Bal("50", "20")))

        val dd2 = DDA(AccountResponse("7567567567", emptyString, traderEori, None, None, viewBalanceIsGranted = false),
          Some(false), Some(false), Some(Limits("200", "100")), None)

        val cashAccountNumber = "987654"
        val cashAccount = CA(AccountResponse(cashAccountNumber, emptyString, traderEori, None, None,
          viewBalanceIsGranted = false), Some("999.99"))

        val accounts = AccountsAndBalancesResponseContainer(
          domain.AccountsAndBalancesResponse(
            Some(domain.AccountResponseCommon(emptyString, Some(emptyString), emptyString, None)),
            domain.AccountResponseDetail(
              Some(emptyString),
              None,
              Some(Seq(dd1, dd2)),
              Some(Seq(guaranteeAccount)),
              Some(Seq(cashAccount))
            )
          )
        )

        when(requestBuilder.withBody(any[AccountsAndBalancesRequestContainer])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[AccountsAndBalancesResponseContainer]], any[ExecutionContext]))
          .thenReturn(Future.successful(accounts))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(appTest) {
          val service = appTest.injector.instanceOf[ApiService]
          await(service.getAccounts(traderEori))
          verify(mockMetricsReporterService).withResponseTimeLogging(eqTo(
            "customs-financials-api.get.accounts"))(any)(any)
        }
      }

      "return all accounts available for the given EORI from the API service for Acc27" in new Setup() {
        when(requestBuilder.withBody(any[AccountsAndBalancesRequestContainer])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[AccountsAndBalancesResponseContainer]], any[ExecutionContext]))
          .thenReturn(Future.successful(traderAccountsWithNoCommonResponse))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.getAccounts(traderEori))
          result must be(traderAccountsWithNoCommonResponse.toCdsAccounts(traderEori))
        }
      }
    }

    "searchAuthorities" should {
      "return NoAuthorities if the API returns 204" in new Setup {
        when(requestBuilder.withBody(any[SearchAuthoritiesRequest])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, emptyString)))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.searchAuthorities(traderEori, traderEori))
          result mustBe Left(NoAuthorities)
        }
      }

      "return SearchError if the API returns an unexpected status" in new Setup {
        when(requestBuilder.withBody(any[SearchAuthoritiesRequest])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, emptyString)))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.searchAuthorities(traderEori, traderEori))
          result mustBe Left(SearchError)
        }
      }

      "return SearchError if the API returns an exception" in new Setup {
        when(requestBuilder.withBody(any[SearchAuthoritiesRequest])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(UpstreamErrorResponse("failure", INTERNAL_SERVER_ERROR)))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.searchAuthorities(traderEori, traderEori))
          result mustBe Left(SearchError)
        }
      }

      "return SearchError if the API returns empty response" in new Setup {
        val httpResponse: JsValue = Json.parse("{}")

        when(requestBuilder.withBody(any[SearchAuthoritiesRequest])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, httpResponse.toString())))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.searchAuthorities(traderEori, traderEori))

          result mustBe Left(SearchError)
        }
      }

      "return SearchedAuthorities if the API returns 200" in new Setup {
        val responseGuarantee: AuthorisedGeneralGuaranteeAccount =
          AuthorisedGeneralGuaranteeAccount(Account("1234", "GeneralGuarantee", "GB000000000000"), Some("10.0"))

        val response: JsValue = Json.toJson(SearchedAuthoritiesResponse("1", None, Some(Seq(responseGuarantee)), None))

        when(requestBuilder.withBody(any[SearchAuthoritiesRequest])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, response.toString())))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val result = await(service.searchAuthorities(traderEori, traderEori))

          result mustBe Right(SearchedAuthorities("1", List(AuthorisedGeneralGuaranteeAccount(Account(
            "1234", "GeneralGuarantee", "GB000000000000"), Some("10.0")))))
        }
      }
    }
  }

  "getNotifications" should {
    "return a Notifications for the given EORI" in new Setup() {
      val notification: List[DocumentAttributes] =
        List(DocumentAttributes(traderEori, C79Certificate, "new file", FILE_SIZE_1000, Map.empty))

      val notifications: SdesNotificationsForEori = SdesNotificationsForEori(traderEori, notification)

      when(requestBuilder.execute(any[HttpReads[SdesNotificationsForEori]], any[ExecutionContext]))
        .thenReturn(Future.successful(notifications))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

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
        DocumentAttributes(traderEori, C79Certificate, "new file", FILE_SIZE_1000, Map.empty),
        DocumentAttributes(traderEori, DutyDefermentStatement, "new file", FILE_SIZE_1000, Map.empty),
        DocumentAttributes(traderEori, SecurityStatement, "new file", FILE_SIZE_1000, Map.empty),
        DocumentAttributes(traderEori, PostponedVATStatement, "new file", FILE_SIZE_1000, Map.empty))

      val notifications = SdesNotificationsForEori(traderEori, notification)
      val mockHttpClient = mock[HttpClientV2]
      val requestBuilder = mock[RequestBuilder]

      val appTest = application().overrides(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[RequestBuilder].toInstance(requestBuilder),
        inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
      ).build()

      when[Future[Seq[DocumentAttributes]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
        .thenReturn(Future.successful(notification))

      when(requestBuilder.execute(any[HttpReads[SdesNotificationsForEori]], any[ExecutionContext]))
        .thenReturn(Future.successful(notifications))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      val service = appTest.injector.instanceOf[ApiService]

      running(appTest) {
        await(service.getEnabledNotifications(traderEori))

        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo(
          "customs-financials-api.get.notifications"))(any)(any)
      }
    }

  }

  "deleteNotification" should {
    "send a delete notification request" in new Setup() {
      when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

      when(mockHttpClient.delete(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        await(service.deleteNotification(traderEori, C79Certificate)(hc))
        verify(mockHttpClient).delete(any)(any)
      }
    }

    "log response time metric" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val traderEori = "12345678"
      val mockHttpClient = mock[HttpClientV2]
      val requestBuilder = mock[RequestBuilder]
      val mockMetricsReporterService = mock[MetricsReporterService]

      val appTest = application().overrides(
        inject.bind[HttpClientV2].toInstance(mockHttpClient),
        inject.bind[RequestBuilder].toInstance(requestBuilder),
        inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
      ).build()

      when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
      when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(HttpResponse.apply(OK, emptyString)))

      when(mockHttpClient.delete(any[URL]())(any())).thenReturn(requestBuilder)

      when[Future[Boolean]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
        .thenReturn(Future.successful(true))

      val service = appTest.injector.instanceOf[ApiService]

      running(appTest) {
        await(service.deleteNotification(traderEori, C79Certificate))

        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo(
          "customs-financials-api.delete.notification"))(any)(any)
      }
    }

    "requestAuthoritiesCsv" should {
      "return OK 200 with requestAcceptedDate" in new Setup {
        val requestAuthoritiesCsvResponse: JsValue = Json.toJson(RequestAuthoritiesCsvResponse("DATE"))

        when(requestBuilder.withBody(any[RequestAuthoritiesCsv])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, requestAuthoritiesCsvResponse.toString)))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.requestAuthoritiesCsv("EORI", Some("someAltEori")))
          response mustBe Right(RequestAuthoritiesCsvResponse("DATE"))
        }
      }

      "return RequestAuthoritiesCSVError when fails" in new Setup {
        when(requestBuilder.withBody(any[RequestAuthoritiesCsv])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "failure")))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.requestAuthoritiesCsv("EORI", Some("someAltEori")))
          response mustBe Left(RequestAuthoritiesCSVError)
        }
      }

      "return JsonParseError when JSResultException thrown parsing json response" in new Setup {
        val jsonError: JsValue = Json.toJson("some" -> "error")

        when(requestBuilder.withBody(any[RequestAuthoritiesCsv])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse.apply(OK, jsonError.toString())))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.requestAuthoritiesCsv("EORI", Some("someAltEori")))
          response mustBe Left(JsonParseError)
        }
      }

      "return RequestAuthoritiesCSVError when exception thrown" in new Setup {
        when(requestBuilder.withBody(any[RequestAuthoritiesCsv])(any(), any(), any()))
          .thenReturn(requestBuilder)

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(UpstreamErrorResponse("failure", INTERNAL_SERVER_ERROR)))

        when(mockHttpClient.post(any)(any)).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.requestAuthoritiesCsv("EORI", Some("someAltEori")))
          response mustBe Left(RequestAuthoritiesCSVError)
        }
      }
    }
  }

  trait Setup {
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val traderEori = "12345678"
    val agentEori = "09876543"

    val guaranteeAccount: GGA = GGA(AccountResponse("G123456", emptyString, traderEori, None, None,
      viewBalanceIsGranted = false), Some("1000000"), Some("200000"))

    val dd1: DDA = DDA(AccountResponse("1231231231", emptyString, traderEori, None, None, viewBalanceIsGranted = false),
      Some(false), Some(false), Some(Limits("200", "100")), Some(Bal("50", "20")))

    val dd2: DDA = DDA(AccountResponse("7567567567", emptyString, traderEori, None, None, viewBalanceIsGranted = false),
      Some(false), Some(false), Some(Limits("200", "100")), None)

    val cashAccountNumber = "987654"
    val cashAccount: CA = CA(AccountResponse(cashAccountNumber, emptyString, traderEori, None, None,
      viewBalanceIsGranted = false), Some("999.99"))

    val traderAccounts: AccountsAndBalancesResponseContainer = AccountsAndBalancesResponseContainer(
      domain.AccountsAndBalancesResponse(
        Some(domain.AccountResponseCommon(emptyString, Some(emptyString), emptyString, None)),
        domain.AccountResponseDetail(
          Some(emptyString),
          None,
          Some(Seq(dd1, dd2)),
          Some(Seq(guaranteeAccount)),
          Some(Seq(cashAccount))
        )
      )
    )

    val traderAccountsWithNoCommonResponse: AccountsAndBalancesResponseContainer = AccountsAndBalancesResponseContainer(
      domain.AccountsAndBalancesResponse(
        None,
        domain.AccountResponseDetail(
          Some(emptyString),
          None,
          Some(Seq(dd1, dd2)),
          Some(Seq(guaranteeAccount)),
          Some(Seq(cashAccount))
        )
      )
    )

    val app: Application = application().overrides(
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[RequestBuilder].toInstance(requestBuilder)
    ).build()

    val service: ApiService = app.injector.instanceOf[ApiService]
  }
}

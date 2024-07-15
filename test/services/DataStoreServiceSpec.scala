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

import config.AppConfig
import domain.{CompanyAddress, EoriHistory, UndeliverableEmail, UndeliverableInformation,
  UndeliverableInformationEvent, UnverifiedEmail, XiEoriAddressInformation}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.invocation.InvocationOnMock
import org.mockito.Mockito.{verify, when}
import play.api.{Application, inject}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.Email
import java.net.URL
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, NotFoundException, ServiceUnavailableException, UpstreamErrorResponse}
import utils.SpecBase
import utils.MustMatchers

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class DataStoreServiceSpec extends SpecBase with MustMatchers {

  "Data store service" should {
    "return json response" in new Setup {
      implicit def stringToOptionLocalDate: String => Option[LocalDate] = in => Some(
        LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))

      val expectedEoriHistory: List[EoriHistory] =
        List(
          EoriHistory("GB11111", "2019-03-01", None),
          EoriHistory("GB22222", "2018-01-01", "2019-02-28")
        )

      val eoriHistory1: EoriHistory = EoriHistory("GB11111", validFrom = "2019-03-01", None)
      val eoriHistory2: EoriHistory = EoriHistory("GB22222", validFrom = "2018-01-01", validUntil = "2019-02-28")
      val eoriHistoryResponse: EoriHistoryResponse = EoriHistoryResponse(Seq(eoriHistory1, eoriHistory2))

      when(requestBuilder.execute(any[HttpReads[EoriHistoryResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(eoriHistoryResponse))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)

        result.toList must be(expectedEoriHistory)
      }
    }

    "handle MDG down" in new Setup {
      val expectedResp: List[EoriHistory] = List(EoriHistory(eori, None, None))

      when(requestBuilder.execute(any[HttpReads[EoriHistoryResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(new ServiceUnavailableException("ServiceUnavailable")))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)
        result mustBe expectedResp

      }
    }

    "have graceful degradation of the historic eori service should return empty eori history" in new Setup {
      val expectedResp: EoriHistoryResponse = EoriHistoryResponse(Seq(EoriHistory(eori, None, None)))

      when(requestBuilder.execute(any[HttpReads[EoriHistoryResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(expectedResp))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)

        result mustBe expectedResp.eoriHistory
      }
    }

    "log response time metric" in new Setup() {
      val expectedResp: EoriHistoryResponse = EoriHistoryResponse(Seq(EoriHistory(eori, None, None)))

      when(requestBuilder.execute(any[HttpReads[EoriHistoryResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(expectedResp))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getAllEoriHistory(eori)
        await(response)

        verify(mockMetricsReporterService).withResponseTimeLogging(ArgumentMatchers.eq(
          "customs-data-store.get.eori-history"))(any)(any)
      }
    }

    "return existing email" in new Setup {
      val jsonResponse: String = """{"address":"someemail@mail.com"}""".stripMargin

      when(requestBuilder.execute(any[HttpReads[EmailResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful((Json.parse(jsonResponse)).as[EmailResponse]))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getEmail(eori)
        val result = await(response)
        result mustBe Right(Email("someemail@mail.com"))
      }
    }

    "return Left(UndeliverableEmail) when there is undeliverable info" in new Setup {
      val undeliverableEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(emptyString,
        emptyString,
        emptyString,
        emptyString,
        None,
        None,
        emptyString)

      val undeliverableInfo: UndeliverableInformation = UndeliverableInformation("test_sub",
        "test_event",
        "test_event",
        LocalDateTime.now(),
        undeliverableEvent)

      val emailAddress = "test"

      val emailResponse: EmailResponse = EmailResponse(Some(emailAddress), None, Some(undeliverableInfo))

      when(requestBuilder.execute(any[HttpReads[EmailResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(emailResponse))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getEmail(eori)
        val result = await(response)

        result mustBe Left(UndeliverableEmail(emailAddress))
      }
    }

    "return Left(UnverifiedEmail) when there is no address in response" in new Setup {
      val undeliverableEvent: UndeliverableInformationEvent = UndeliverableInformationEvent(emptyString,
        emptyString,
        emptyString,
        emptyString,
        None,
        None,
        emptyString)

      val undeliverableInfo: UndeliverableInformation = UndeliverableInformation("test_sub",
        "test_event",
        "test_event",
        LocalDateTime.now(),
        undeliverableEvent)

      val emailResponse: EmailResponse = EmailResponse(None, None, Some(undeliverableInfo))

      when(requestBuilder.execute(any[HttpReads[EmailResponse]], any[ExecutionContext]))
        .thenReturn(Future.successful(emailResponse))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getEmail(eori)
        val result = await(response)

        result mustBe Left(UnverifiedEmail)
      }
    }

    "return a UnverifiedEmail" in new Setup {
      when(requestBuilder.execute(any[HttpReads[EmailResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(UpstreamErrorResponse("NoData", NOT_FOUND, NOT_FOUND)))

      when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

      running(app) {
        val response = service.getEmail(eori)
        await(response) mustBe Left(UnverifiedEmail)

      }
    }

    "throw service unavailable" in new Setup {
      running(app) {
        val eori = "ETMP500ERROR"

        when(requestBuilder.execute(any[HttpReads[EmailResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new ServiceUnavailableException("ServiceUnavailable")))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        assertThrows[ServiceUnavailableException](await(service.getEmail(eori)))
      }
    }

    "CompanyName" should {
      "return company name" in new Setup {
        val companyName = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "1", address)

        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(companyInformationResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getCompanyName(eori)
          val result = await(response)
          result must be(Some(companyName))
        }
      }

      "return None when consent is not given" in new Setup {
        val companyName = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "0", address)

        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(companyInformationResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getCompanyName(eori)
          val result = await(response)
          result mustBe None
        }
      }

      "return None when no company information is found" in new Setup {
        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new NotFoundException("Not Found Company Information")))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.getCompanyName(eori))
          response mustBe None
        }
      }
    }

    "OwnCompanyName" should {
      "return own company name" in new Setup {
        val companyName = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "0", address)

        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(companyInformationResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getOwnCompanyName(eori)
          val result = await(response)
          result must be(Some(companyName))
        }
      }

      "return None when no company information is found" in new Setup {
        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
        .thenReturn(Future.failed(new NotFoundException("Not Found Company Information")))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.getOwnCompanyName(eori))
          response mustBe None
        }
      }
    }

    "CompanyAddress" should {
      "return Company Address" in new Setup {
        val companyName = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
        val companyInformationResponse: CompanyInformationResponse = CompanyInformationResponse(companyName, "1", address)

        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(companyInformationResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getCompanyAddress(eori)
          val result = await(response)
          result must be(Some(address))
        }
      }

      "return None when no Address Found" in new Setup {
        when(requestBuilder.execute(any[HttpReads[CompanyInformationResponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new NotFoundException("Not Found Company Address")))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.getCompanyAddress(eori))
          response mustBe None
        }
      }
    }

    "XiEori" should {
      "return xi eori" in new Setup {
        val xiEori = "XI123456789"
        val xiAddress: XiEoriAddressInformation =
          XiEoriAddressInformation("Street1", None, Some("City"), Some("GB"), Some("Post Code"))
        val xiEoriResponse: XiEoriInformationReponse = XiEoriInformationReponse(xiEori, "S", xiAddress)

        when(requestBuilder.execute(any[HttpReads[XiEoriInformationReponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(xiEoriResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getXiEori(eori)
          val result = await(response)
          result must be(Some(xiEori))
        }
      }

      "return None when no xi Eori is found" in new Setup {
        when(requestBuilder.execute(any[HttpReads[XiEoriInformationReponse]], any[ExecutionContext]))
          .thenReturn(Future.failed(new NotFoundException("Not Found Xi EORI Information")))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = await(service.getXiEori(eori))
          response mustBe None
        }
      }

      "return None when retunred xi Eori is empty" in new Setup {
        val xiEori: String = emptyString
        val xiAddress: XiEoriAddressInformation =
          XiEoriAddressInformation("Street1", None, Some("City"), Some("GB"), Some("Post Code"))
        val xiEoriResponse: XiEoriInformationReponse = XiEoriInformationReponse(xiEori, "S", xiAddress)

        when(requestBuilder.execute(any[HttpReads[XiEoriInformationReponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(xiEoriResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        running(app) {
          val response = service.getXiEori(eori)
          val result = await(response)

          result mustBe None
        }
      }

      "return None when feature flag is false" in {
        val mockMetricsReporterService = mock[MetricsReporterService]
        val mockHttpClient = mock[HttpClientV2]
        val requestBuilder: RequestBuilder = mock[RequestBuilder]
        val mockAppConfig = mock[AppConfig]
        implicit val hc: HeaderCarrier = HeaderCarrier()

        val eori = "GB11111"
        val xiEori = "XI123456789"
        val xiAddress = XiEoriAddressInformation("Street1", None, Some("City"), Some("GB"), Some("Post Code"))
        val xiEoriResponse = XiEoriInformationReponse(xiEori, "S", xiAddress)

        when(requestBuilder.execute(any[HttpReads[XiEoriInformationReponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(xiEoriResponse))

        when(mockHttpClient.get(any[URL]())(any())).thenReturn(requestBuilder)

        when(mockMetricsReporterService.withResponseTimeLogging[Seq[EoriHistory]](any)(any)(any))
          .thenAnswer((i: InvocationOnMock) => {
            i.getArgument[Future[Seq[EoriHistory]]](1)
          })

        when(mockAppConfig.customsDataStore).thenReturn("test/value")
        when(mockAppConfig.xiEoriEnabled).thenReturn(false)

        val app = application().overrides(
          inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService),
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[RequestBuilder].toInstance(requestBuilder),
          inject.bind[AppConfig].toInstance(mockAppConfig)
        ).build()

        val service = app.injector.instanceOf[DataStoreService]

        val response = service.getXiEori(eori)
        val result = await(response)

        result mustBe None
      }
    }
  }

  trait Setup {
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val eori = "GB11111"

    val app: Application = application().overrides(
      inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService),
      inject.bind[HttpClientV2].toInstance(mockHttpClient),
      inject.bind[RequestBuilder].toInstance(requestBuilder)
    ).build()

    val service: DataStoreService = app.injector.instanceOf[DataStoreService]

    when(mockMetricsReporterService.withResponseTimeLogging[Seq[EoriHistory]](any)(any)(any))
      .thenAnswer((i: InvocationOnMock) => {
        i.getArgument[Future[Seq[EoriHistory]]](1)
      })
  }
}

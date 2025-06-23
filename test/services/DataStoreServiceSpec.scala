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
import domain.{
  CompanyAddress, EmailResponses, EoriHistory, UndeliverableEmail, UndeliverableInformation,
  UndeliverableInformationEvent, UnverifiedEmail, XiEoriAddressInformation
}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.invocation.InvocationOnMock
import org.mockito.Mockito.{verify, when}
import play.api.{Application, Configuration, inject}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.retrieve.Email

import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import utils.SpecBase
import utils.MustMatchers
import com.github.tomakehurst.wiremock.client.WireMock.{get, notFound, ok, serviceUnavailable, urlPathMatching}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.Future
import utils.WireMockSupportProvider
import com.typesafe.config.ConfigFactory

class DataStoreServiceSpec extends SpecBase with MustMatchers with WireMockSupportProvider {

  "Data store service" should {
    "return json response" in new Setup {
      implicit def stringToOptionLocalDate: String => Option[LocalDate] =
        in => Some(LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))

      val expectedEoriHistory: List[EoriHistory] =
        List(
          EoriHistory("GB11111", "2019-03-01", None),
          EoriHistory("GB22222", "2018-01-01", "2019-02-28")
        )

      val eoriHistory1: EoriHistory                = EoriHistory("GB11111", validFrom = "2019-03-01", None)
      val eoriHistory2: EoriHistory                = EoriHistory("GB22222", validFrom = "2018-01-01", validUntil = "2019-02-28")
      val eoriHistoryResponse: EoriHistoryResponse = EoriHistoryResponse(Seq(eoriHistory1, eoriHistory2))

      wireMockServer.stubFor(
        get(urlPathMatching(eoriHistoryUrl))
          .willReturn(
            ok(Json.toJson(eoriHistoryResponse).toString)
          )
      )

      val result: Seq[EoriHistory] = await(service.getAllEoriHistory(eori))

      result.toList must be(expectedEoriHistory)
      verifyEndPointUrlHit(eoriHistoryUrl)
    }

    "handle MDG down" in new Setup {
      val expectedResp: List[EoriHistory] = List(EoriHistory(eori, None, None))

      wireMockServer.stubFor(
        get(urlPathMatching(eoriHistoryUrl))
          .willReturn(
            serviceUnavailable()
          )
      )

      val result: Seq[EoriHistory] = await(service.getAllEoriHistory(eori))

      result mustBe expectedResp
      verifyEndPointUrlHit(eoriHistoryUrl)
    }

    "handle 404" in new Setup {
      val expectedResp: List[EoriHistory] = List(EoriHistory(eori, None, None))

      wireMockServer.stubFor(
        get(urlPathMatching(eoriHistoryUrl))
          .willReturn(
            notFound()
          )
      )

      val result: Seq[EoriHistory] = await(service.getAllEoriHistory(eori))

      result mustBe expectedResp
      verifyEndPointUrlHit(eoriHistoryUrl)
    }

    "have graceful degradation of the historic eori service should return empty eori history" in new Setup {
      val expectedResp: EoriHistoryResponse = EoriHistoryResponse(Seq(EoriHistory(eori, None, None)))

      wireMockServer.stubFor(
        get(urlPathMatching(eoriHistoryUrl))
          .willReturn(
            ok(Json.toJson(expectedResp).toString)
          )
      )

      val result: Seq[EoriHistory] = await(service.getAllEoriHistory(eori))

      result mustBe expectedResp.eoriHistory
      verifyEndPointUrlHit(eoriHistoryUrl)
    }

    "log response time metric" in new Setup() {
      val expectedResp: EoriHistoryResponse = EoriHistoryResponse(Seq(EoriHistory(eori, None, None)))

      wireMockServer.stubFor(
        get(urlPathMatching(eoriHistoryUrl))
          .willReturn(
            ok(Json.toJson(expectedResp).toString)
          )
      )

      val response: Future[Seq[EoriHistory]] = service.getAllEoriHistory(eori)
      await(response)

      verify(mockMetricsReporterService).withResponseTimeLogging(
        ArgumentMatchers.eq("customs-data-store.get.eori-history")
      )(any)(any)

      verifyEndPointUrlHit(eoriHistoryUrl)
    }

    "return existing email" in new Setup {
      val jsonResponse: String = """{"address":"someemail@mail.com"}""".stripMargin

      wireMockServer.stubFor(
        get(urlPathMatching(getEmailUrl))
          .willReturn(
            ok(jsonResponse)
          )
      )

      val result: Either[EmailResponses, Email] = await(service.getEmail)

      result mustBe Right(Email("someemail@mail.com"))
      verifyEndPointUrlHit(getEmailUrl)
    }

    "return Left(UndeliverableEmail) when there is undeliverable info" in new Setup {
      val undeliverableEvent: UndeliverableInformationEvent =
        UndeliverableInformationEvent(emptyString, emptyString, emptyString, emptyString, None, None, emptyString)

      val undeliverableInfo: UndeliverableInformation =
        UndeliverableInformation("test_sub", "test_event", "test_event", LocalDateTime.now(), undeliverableEvent)

      val emailAddress = "test"

      val emailResponse: EmailResponse = EmailResponse(Some(emailAddress), None, Some(undeliverableInfo))

      wireMockServer.stubFor(
        get(urlPathMatching(getEmailUrl))
          .willReturn(
            ok(Json.toJson(emailResponse).toString)
          )
      )

      val result: Either[EmailResponses, Email] = await(service.getEmail)

      result mustBe Left(UndeliverableEmail(emailAddress))
      verifyEndPointUrlHit(getEmailUrl)
    }

    "return Left(UnverifiedEmail) when there is no address in response" in new Setup {
      val undeliverableEvent: UndeliverableInformationEvent =
        UndeliverableInformationEvent(emptyString, emptyString, emptyString, emptyString, None, None, emptyString)

      val undeliverableInfo: UndeliverableInformation =
        UndeliverableInformation("test_sub", "test_event", "test_event", LocalDateTime.now(), undeliverableEvent)

      val emailResponse: EmailResponse = EmailResponse(None, None, Some(undeliverableInfo))

      wireMockServer.stubFor(
        get(urlPathMatching(getEmailUrl))
          .willReturn(
            ok(Json.toJson(emailResponse).toString)
          )
      )

      val result: Either[EmailResponses, Email] = await(service.getEmail)

      result mustBe Left(UnverifiedEmail)
      verifyEndPointUrlHit(getEmailUrl)
    }

    "return a UnverifiedEmail" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(getEmailUrl))
          .willReturn(notFound())
      )

      val response: Future[Either[EmailResponses, Email]] = service.getEmail

      await(response) mustBe Left(UnverifiedEmail)
      verifyEndPointUrlHit(getEmailUrl)
    }

    "throw service unavailable" in new Setup {

      wireMockServer.stubFor(
        get(urlPathMatching(getEmailUrl))
          .willReturn(serviceUnavailable())
      )

      assertThrows[UpstreamErrorResponse](await(service.getEmail))

      verifyEndPointUrlHit(getEmailUrl)
    }

    "CompanyName" should {
      "return company name" in new Setup {
        val companyName                                            = "Company name"
        val address: CompanyAddress                                = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "1", address)

        wireMockServer.stubFor(
          get(urlPathMatching(getCompanyNameUrl))
            .willReturn(
              ok(Json.toJson(companyInformationResponse).toString)
            )
        )

        val result: Option[String] = await(service.getCompanyName(eori))

        result must be(Some(companyName))
        verifyEndPointUrlHit(getCompanyNameUrl)
      }

      "return None when consent is not given" in new Setup {
        val companyName             = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")

        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "0", address)

        wireMockServer.stubFor(
          get(urlPathMatching(getCompanyNameUrl))
            .willReturn(
              ok(Json.toJson(companyInformationResponse).toString)
            )
        )

        val result: Option[String] = await(service.getCompanyName(eori))

        result mustBe empty
        verifyEndPointUrlHit(getCompanyNameUrl)
      }

      "return None when no company information is found" in new Setup {
        wireMockServer.stubFor(
          get(urlPathMatching(getCompanyNameUrl))
            .willReturn(notFound())
        )

        val response: Option[String] = await(service.getCompanyName(eori))

        response mustBe empty
        verifyEndPointUrlHit(getCompanyNameUrl)
      }
    }

    "OwnCompanyName" should {
      "return own company name" in new Setup {
        val companyName             = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")

        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "0", address)

        wireMockServer.stubFor(
          get(urlPathMatching(getOwnCompanyNameUrl))
            .willReturn(
              ok(Json.toJson(companyInformationResponse).toString)
            )
        )

        val result: Option[String] = await(service.getOwnCompanyName)

        result must be(Some(companyName))
        verifyEndPointUrlHit(getOwnCompanyNameUrl)
      }

      "return None when no company information is found" in new Setup {
        wireMockServer.stubFor(
          get(urlPathMatching(getOwnCompanyNameUrl))
            .willReturn(notFound())
        )

        val response: Option[String] = await(service.getOwnCompanyName)

        response mustBe empty
        verifyEndPointUrlHit(getOwnCompanyNameUrl)
      }
    }

    "CompanyAddress" should {
      "return Company Address" in new Setup {
        val companyName             = "Company name"
        val address: CompanyAddress = CompanyAddress("Street", "City", Some("Post Code"), "Country code")

        val companyInformationResponse: CompanyInformationResponse =
          CompanyInformationResponse(companyName, "1", address)

        wireMockServer.stubFor(
          get(urlPathMatching(getCompanyAddressUrl))
            .willReturn(
              ok(Json.toJson(companyInformationResponse).toString)
            )
        )

        val result: Option[CompanyAddress] = await(service.getCompanyAddress)

        result must be(Some(address))
        verifyEndPointUrlHit(getCompanyAddressUrl)
      }

      "return None when no Address Found" in new Setup {
        wireMockServer.stubFor(
          get(urlPathMatching(getCompanyAddressUrl))
            .willReturn(notFound())
        )

        val response: Option[CompanyAddress] = await(service.getCompanyAddress)

        response mustBe empty
        verifyEndPointUrlHit(getCompanyAddressUrl)
      }
    }

    "XiEori" should {
      "return xi eori" in new Setup {
        val xiEori = "XI123456789"

        val xiAddress: XiEoriAddressInformation =
          XiEoriAddressInformation("Street1", None, Some("City"), Some("GB"), Some("Post Code"))

        val xiEoriResponse: XiEoriInformationReponse = XiEoriInformationReponse(xiEori, "S", xiAddress)

        wireMockServer.stubFor(
          get(urlPathMatching(xiEoriInfoUrl))
            .willReturn(
              ok(Json.toJson(xiEoriResponse).toString)
            )
        )

        val result: Option[String] = await(service.getXiEori)

        result must be(Some(xiEori))
        verifyEndPointUrlHit(xiEoriInfoUrl)
      }

      "return None when no xi Eori is found" in new Setup {
        wireMockServer.stubFor(
          get(urlPathMatching(xiEoriInfoUrl))
            .willReturn(notFound())
        )

        val response: Option[String] = await(service.getXiEori)

        response mustBe empty
        verifyEndPointUrlHit(xiEoriInfoUrl)
      }

      "return None when returned xi Eori is empty" in new Setup {
        val xiEori: String = emptyString

        val xiAddress: XiEoriAddressInformation =
          XiEoriAddressInformation("Street1", None, Some("City"), Some("GB"), Some("Post Code"))

        val xiEoriResponse: XiEoriInformationReponse = XiEoriInformationReponse(xiEori, "S", xiAddress)

        wireMockServer.stubFor(
          get(urlPathMatching(xiEoriInfoUrl))
            .willReturn(
              ok(Json.toJson(xiEoriResponse).toString)
            )
        )

        val result: Option[String] = await(service.getXiEori)

        result mustBe empty
        verifyEndPointUrlHit(xiEoriInfoUrl)
      }

      "return None when feature flag is false" in new Setup {
        override val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]
        val mockAppConfig: AppConfig                                    = mock[AppConfig]

        override val app: Application = application()
          .configure(config)
          .overrides(
            inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService),
            inject.bind[AppConfig].toInstance(mockAppConfig)
          )
          .build()

        when(mockMetricsReporterService.withResponseTimeLogging[Seq[EoriHistory]](any)(any)(any))
          .thenAnswer { (i: InvocationOnMock) =>
            i.getArgument[Future[Seq[EoriHistory]]](1)
          }

        when(mockAppConfig.customsDataStore).thenReturn("test/value")
        when(mockAppConfig.xiEoriEnabled).thenReturn(false)

        override val service: DataStoreService = app.injector.instanceOf[DataStoreService]

        val result: Option[String] = await(service.getXiEori)

        result mustBe empty
        verifyEndPointUrlHit(xiEoriInfoUrl, 0)
      }
    }
  }

  override def config: Configuration = Configuration(
    ConfigFactory.parseString(
      s"""
         |microservice {
         |  services {
         |      customs-data-store {
         |      protocol = http
         |      host     = $wireMockHost
         |      port     = $wireMockPort
         |      context = "/customs-data-store"
         |    }
         |  }
         |}
         |""".stripMargin
    )
  )

  trait Setup {
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    implicit val hc: HeaderCarrier = HeaderCarrier()
    val eori                       = "GB11111"

    val getEmailUrl: String    = "/customs-data-store/eori/verified-email"
    val eoriHistoryUrl: String = "/customs-data-store/eori/eori-history"
    val getCompanyNameUrl      = s"/customs-data-store/eori/$eori/company-information"
    val getOwnCompanyNameUrl   = "/customs-data-store/eori/company-information"
    val getCompanyAddressUrl   = "/customs-data-store/eori/company-information"
    val xiEoriInfoUrl          = "/customs-data-store/eori/xieori-information"

    val app: Application = application()
      .configure(config)
      .overrides(
        inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService)
      )
      .build()

    val service: DataStoreService = app.injector.instanceOf[DataStoreService]

    when(mockMetricsReporterService.withResponseTimeLogging[Seq[EoriHistory]](any)(any)(any))
      .thenAnswer { (i: InvocationOnMock) =>
        i.getArgument[Future[Seq[EoriHistory]]](1)
      }
  }
}

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

package services

import domain.{CompanyAddress, EoriHistory, UnverifiedEmail}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.invocation.InvocationOnMock
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, NotFoundException, ServiceUnavailableException, UpstreamErrorResponse}
import utils.SpecBase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class DataStoreServiceSpec extends SpecBase {

  trait Setup {
    val mockMetricsReporterService = mock[MetricsReporterService]
    val mockHttp = mock[HttpClient]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val app = application().overrides(
      inject.bind[MetricsReporterService].toInstance(mockMetricsReporterService),
      inject.bind[HttpClient].toInstance(mockHttp)
    ).build()

    val service = app.injector.instanceOf[DataStoreService]

    when(mockMetricsReporterService.withResponseTimeLogging[Seq[EoriHistory]](any)(any)(any))
      .thenAnswer((i: InvocationOnMock) => {
        i.getArgument[Future[Seq[EoriHistory]]](1)
      })
  }


  "Data store service" should {
    "return json response" in new Setup {
      val eori = "GB11111"

      implicit def stringToOptionLocalDate: String => Option[LocalDate] = in => Some(LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))

      val expectedEoriHistory = List(EoriHistory("GB11111", "2019-03-01", None), EoriHistory("GB22222", "2018-01-01", "2019-02-28"))
      val eoriHistory1 = EoriHistory("GB11111", validFrom = "2019-03-01", None)
      val eoriHistory2 = EoriHistory("GB22222", validFrom = "2018-01-01", validUntil = "2019-02-28")

      val eoriHistoryResponse = EoriHistoryResponse(Seq(eoriHistory1, eoriHistory2))
      when[Future[EoriHistoryResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.successful(eoriHistoryResponse))
      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)
        result.toList must be(expectedEoriHistory)
      }
    }

    "handle MDG down" in new Setup {
      val eori = "GB11111"
      val expectedResp = List(EoriHistory(eori, None, None))
      when[Future[EoriHistoryResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.failed(new ServiceUnavailableException("ServiceUnavailable")))
      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)
        result mustBe expectedResp

      }
    }

    "have graceful degradation of the historic eori service should return empty eori history" in new Setup {
      val eori = "GB11111"
      val expectedResp = EoriHistoryResponse(Seq(EoriHistory("GB11111", None, None)))
      when[Future[EoriHistoryResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.successful(expectedResp))
      running(app) {
        val response = service.getAllEoriHistory(eori)
        val result = await(response)
        result mustBe expectedResp.eoriHistory
      }
    }

    "log response time metric" in new Setup() {
      val eori = "GB11111"
      val expectedResp = EoriHistoryResponse(Seq(EoriHistory("GB11111", None, None)))
      when[Future[EoriHistoryResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.successful(expectedResp))
      running(app) {
        val response = service.getAllEoriHistory(eori)
        await(response)
        verify(mockMetricsReporterService).withResponseTimeLogging(ArgumentMatchers.eq("customs-data-store.get.eori-history"))(any)(any)
      }
    }

    "return existing email" in new Setup {
      val eori = "GB11111"

      val jsonResponse = """{"address":"someemail@mail.com"}""".stripMargin

      when[Future[EmailResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.successful(Json.parse(jsonResponse).as[EmailResponse]))

      running(app) {
        val response = service.getEmail(eori)
        val result = await(response)
        result mustBe Right(Email("someemail@mail.com"))
      }
    }

    "return a UnverifiedEmail" in new Setup {
      val eori = "GB11111"
      when[Future[EmailResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.failed(UpstreamErrorResponse("NoData", 404, 404)))

      running(app) {
        val response = service.getEmail(eori)
        await(response) mustBe Left(UnverifiedEmail)

      }
    }

    "throw service unavailable" in new Setup {
      running(app) {
        val eori = "ETMP500ERROR"
        when[Future[EmailResponse]](mockHttp.GET(any, any, any)(any, any, any)).thenReturn(Future.failed(new ServiceUnavailableException("ServiceUnavailable")))
        assertThrows[ServiceUnavailableException](await(service.getEmail(eori)))
      }
    }

    "return company name" in new Setup {
      val eori = "GB11111"
      val companyName = "Company name"
      val address = CompanyAddress("Street", "City", Some("Post Code"), "Country code")
      val companyInformationResponse = CompanyInformationResponse(companyName, "1", address)
      when[Future[CompanyInformationResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.successful(companyInformationResponse))

      running(app) {
        val response = service.getCompanyName(eori)
        val result = await(response)
        result must be(Some(companyName))
      }
    }

    "return None when no company information is found" in new Setup {
      val eori = "GB11111"
      when[Future[CompanyInformationResponse]](mockHttp.GET(any, any, any)(any, any, any))
        .thenReturn(Future.failed(new NotFoundException("Not Found Company Information")))

      running(app) {
        val response = await(service.getCompanyName(eori))
        response mustBe None
      }
    }
  }
}

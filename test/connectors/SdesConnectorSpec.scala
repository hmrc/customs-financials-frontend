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

package connectors

import config.AppConfig
import domain.FileFormat.Csv
import domain.FileRole.StandingAuthority
import domain.{FileInformation, Metadata, MetadataItem, StandingAuthorityFile, StandingAuthorityMetadata}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.i18n.Messages
import play.api.inject
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers._
import services.{AuditingService, MetricsReporterService, SdesGatekeeperService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.SpecBase

import scala.concurrent.Future

//scalastyle:off magic.number
class SdesConnectorSpec extends SpecBase {

  "HttpSdesConnector" should {
    "getAuthoritiesCsvFiles" should {
      "make a GET request to sdesStandingAuthorityFilesUrl" in new Setup {
        val url = sdesStandingAuthorityFileUrl

        val app = application().overrides(
          inject.bind[HttpClient].toInstance(mockHttp)
        ).build()

        val sdesService = app.injector.instanceOf[SdesConnector]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url), any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getAuthoritiesCsvFiles(someEori)(hc))
        verify(mockHttp).GET(eqTo(url), any, any)(any, any, any)
      }

      "converts Sdes response to List[StandingAuthorityFile]" in new Setup {
        val url = sdesStandingAuthorityFileUrl
        val numberOfStatements = standingAuthoritiesFilesSdesResponse.length

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url), any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK,
            Json.toJson(standingAuthoritiesFilesSdesResponse).toString())))

        when(sdesGatekeeperServiceSpy.convertTo(any)).thenCallRealMethod()

        val app = application().overrides(
          inject.bind[HttpClient].toInstance(mockHttp),
          inject.bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()

        val sdesService = app.injector.instanceOf[SdesConnector]

        await(sdesService.getAuthoritiesCsvFiles(someEori)(hc))
        verify(mockHttp).GET(eqTo(url), any, any)(any, any, any)
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToStandingAuthoritiesFile(any)
      }

      "filter out unknown file types" in new Setup {
        val url = sdesStandingAuthorityFileUrl

        val app = application().overrides(
          inject.bind[HttpClient].toInstance(mockHttp)
        ).build()

        val sdesService = app.injector.instanceOf[SdesConnector]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url), any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK,
            Json.toJson(standingAuthoritiesFilesWithUnknownFiletypesSdesResponse).toString())))

        val result = await(sdesService.getAuthoritiesCsvFiles(someEoriWithUnknownFileTypes)(hc))
        result mustBe (standingAuthorityFiles)
        verify(mockHttp).GET(eqTo(url), any, any)(any, any, any)
      }
    }
  }

  trait Setup {
    val hc: HeaderCarrier = HeaderCarrier()
    implicit val messages: Messages = stubMessages()
    val someEori = "12345678"
    val someEoriWithUnknownFileTypes = "EoriFooBar"
    val xClientId = "TheClientId"
    val xClientIdHeader = "x-client-id"
    val xSDESKey = "X-SDES-Key"

    val sdesStandingAuthorityFileUrl =
      "http://localhost:9754/customs-financials-sdes-stub/files-available/list/StandingAuthority"

    val standingAuthorityFiles = List(
      StandingAuthorityFile("name_01", "download_url_01", 111L,
        StandingAuthorityMetadata(2022, 6, 1, Csv, StandingAuthority), ""),
      StandingAuthorityFile("name_02", "download_url_02", 115L,
        StandingAuthorityMetadata(2022, 5, 25, Csv, StandingAuthority), "")
    )

    val standingAuthoritiesFilesSdesResponse = List(
      FileInformation("name_01", "download_url_01", 111L,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("PeriodStartDay", "1"), MetadataItem("FileType", "csv"),
          MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_02", "download_url_02", 115L,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "5"),
          MetadataItem("PeriodStartDay", "25"), MetadataItem("FileType", "csv"),
          MetadataItem("FileRole", "StandingAuthority"))))
    )

    val standingAuthoritiesFilesWithUnknownFiletypesSdesResponse = List(
      FileInformation("name_01", "download_url_01", 111L,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "6"), MetadataItem("PeriodStartDay", "1"),
          MetadataItem("FileType", "csv"), MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_02", "download_url_02", 115L,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "5"), MetadataItem("PeriodStartDay", "25"),
          MetadataItem("FileType", "csv"), MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_03", "download_url_03", 115L,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "4"), MetadataItem("PeriodStartDay", "25"),
          MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "StandingAuthority"))))
    )

    val sdesGatekeeperServiceSpy = spy(new SdesGatekeeperService())
    val mockHttp = mock[HttpClient]
    val mockAppConfig = mock[AppConfig]
    val mockMetricsReporterService = mock[MetricsReporterService]
    val mockAuditingService = mock[AuditingService]
  }
}

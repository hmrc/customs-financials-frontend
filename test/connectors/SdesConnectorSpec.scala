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
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{spy, times, verify, when}

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status
import play.api.i18n.Messages
import play.api.{Application, inject}
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.*
import services.{AuditingService, MetricsReporterService, SdesGatekeeperService}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import utils.SpecBase
import utils.MustMatchers

import utils.TestData.{DAY_1, DAY_25, FILE_SIZE_111, FILE_SIZE_115, MONTH_5, MONTH_6, YEAR_2022}

class SdesConnectorSpec extends SpecBase with MustMatchers {

  "HttpSdesConnector" should {

    "getAuthoritiesCsvFiles" should {

      "make a GET request to sdesStandingAuthorityFilesUrl" in new Setup {
        val url: String = sdesStandingAuthorityFileUrl

        val app: Application = application().overrides(
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[RequestBuilder].toInstance(requestBuilder)
        ).build()

        val sdesService: SdesConnector = app.injector.instanceOf[SdesConnector]

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        when(mockHttpClient.get(eqTo(url"$url"))(any())).thenReturn(requestBuilder)

        await(sdesService.getAuthoritiesCsvFiles(someEori)(hc))

        verify(mockHttpClient).get(eqTo(url"$url"))(any)
      }

      "converts Sdes response to List[StandingAuthorityFile]" in new Setup {
        val url: String = sdesStandingAuthorityFileUrl
        val numberOfStatements: Int = standingAuthoritiesFilesSdesResponse.length

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(
            Future.successful(HttpResponse(Status.OK, Json.toJson(standingAuthoritiesFilesSdesResponse).toString()))
          )

        when(mockHttpClient.get(eqTo(url"$url"))(any())).thenReturn(requestBuilder)

        when(sdesGatekeeperServiceSpy.convertTo(any())).thenCallRealMethod()

        val app: Application = application().overrides(
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[RequestBuilder].toInstance(requestBuilder),
          inject.bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()

        val sdesService: SdesConnector = app.injector.instanceOf[SdesConnector]

        await(sdesService.getAuthoritiesCsvFiles(someEori)(hc))
        verify(mockHttpClient).get(eqTo(url"$url"))(any)
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToStandingAuthoritiesFile(any)
      }

      "filter out unknown file types" in new Setup {
        val url: String = sdesStandingAuthorityFileUrl

        val app: Application = application().overrides(
          inject.bind[HttpClientV2].toInstance(mockHttpClient),
          inject.bind[RequestBuilder].toInstance(requestBuilder)
        ).build()

        val sdesService: SdesConnector = app.injector.instanceOf[SdesConnector]

        when(requestBuilder.execute(any[HttpReads[HttpResponse]], any[ExecutionContext]))
          .thenReturn(
            Future.successful(
              HttpResponse(Status.OK, Json.toJson(standingAuthoritiesFilesWithUnknownFiletypesSdesResponse).toString()))
          )

        when(mockHttpClient.get(eqTo(url"$url"))(any())).thenReturn(requestBuilder)

        val result: Seq[StandingAuthorityFile] =
          await(sdesService.getAuthoritiesCsvFiles(someEoriWithUnknownFileTypes)(hc))

        result mustBe (standingAuthorityFiles)
        verify(mockHttpClient).get(eqTo(url"$url"))(any)
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

    val standingAuthorityFiles: List[StandingAuthorityFile] = List(
      StandingAuthorityFile("name_01", "download_url_01", FILE_SIZE_111,
        StandingAuthorityMetadata(YEAR_2022, MONTH_6, DAY_1, Csv, StandingAuthority), emptyString),
      StandingAuthorityFile("name_02", "download_url_02", FILE_SIZE_115,
        StandingAuthorityMetadata(YEAR_2022, MONTH_5, DAY_25, Csv, StandingAuthority), emptyString)
    )

    val standingAuthoritiesFilesSdesResponse: List[FileInformation] = List(
      FileInformation("name_01", "download_url_01", FILE_SIZE_111,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "6"),
          MetadataItem("PeriodStartDay", "1"), MetadataItem("FileType", "csv"),
          MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_02", "download_url_02", FILE_SIZE_115,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "5"),
          MetadataItem("PeriodStartDay", "25"), MetadataItem("FileType", "csv"),
          MetadataItem("FileRole", "StandingAuthority"))))
    )

    val standingAuthoritiesFilesWithUnknownFiletypesSdesResponse: List[FileInformation] = List(
      FileInformation("name_01", "download_url_01", FILE_SIZE_111,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "6"), MetadataItem("PeriodStartDay", "1"),
          MetadataItem("FileType", "csv"), MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_02", "download_url_02", FILE_SIZE_115,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "5"), MetadataItem("PeriodStartDay", "25"),
          MetadataItem("FileType", "csv"), MetadataItem("FileRole", "StandingAuthority")))),
      FileInformation("name_03", "download_url_03", FILE_SIZE_115,
        Metadata(List(MetadataItem("PeriodStartYear", "2022"),
          MetadataItem("PeriodStartMonth", "4"), MetadataItem("PeriodStartDay", "25"),
          MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "StandingAuthority"))))
    )

    val sdesGatekeeperServiceSpy: SdesGatekeeperService = spy(new SdesGatekeeperService())
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
    val mockAppConfig: AppConfig = mock[AppConfig]
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]
    val mockAuditingService: AuditingService = mock[AuditingService]
  }
}

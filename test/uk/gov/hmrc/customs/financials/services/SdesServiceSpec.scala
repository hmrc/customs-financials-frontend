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

package uk.gov.hmrc.customs.financials.services

import play.api.http.Status
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.libs.json._
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain
import uk.gov.hmrc.customs.financials.domain.DDStatementType.Weekly
import uk.gov.hmrc.customs.financials.domain.DutyPaymentMethod.CDS
import uk.gov.hmrc.customs.financials.domain.FileFormat.{Csv, Pdf}
import uk.gov.hmrc.customs.financials.domain.FileRole._
import uk.gov.hmrc.customs.financials.domain.{MetadataItem, _}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.Future

//noinspection TypeAnnotation
//scalastyle:off magic.number public.methods.have.type
class SdesServiceSpec extends SpecBase {
  trait Setup {
    val hc: HeaderCarrier = HeaderCarrier()
    implicit val messages: Messages = Helpers.stubMessages()
    val someEori = "12345678"
    val someEoriWithUnknownFileTypes = "EoriFooBar"
    val someDan = "87654321"

    val xClientId = "TheClientId"

    val xClientIdHeader = "x-client-id"
    val xSDESKey = "X-SDES-Key"

    val sdesVatCertificatesUrl = "http://localhost:9754/customs-financials-sdes-stub/files-available/list/C79Certificate"
    val sdesPostponedVatStatementsUrl = "http://localhost:9754/customs-financials-sdes-stub/files-available/list/PostponedVATStatement"
    val sdesSecurityStatementsUrl = "http://localhost:9754/customs-financials-sdes-stub/files-available/list/SecurityStatement"
    val sdesDutyDefermentStatementsUrl = "http://localhost:9754/customs-financials-sdes-stub/files-available/list/DutyDefermentStatement"

    val vatCertificateFiles = List(
      VatCertificateFile("name_04", "download_url_06", 111L, VatCertificateFileMetadata(2018, 3, Pdf, C79Certificate, None), ""),
      VatCertificateFile("name_04", "download_url_05", 111L, VatCertificateFileMetadata(2018, 4, Csv, C79Certificate, None), ""),
      VatCertificateFile("name_04", "download_url_04", 111L, VatCertificateFileMetadata(2018, 4, Pdf, C79Certificate, None), ""),
      VatCertificateFile("name_03", "download_url_03", 111L, VatCertificateFileMetadata(2018, 5, Pdf, C79Certificate, None), ""),
      VatCertificateFile("name_02", "download_url_02", 111L, VatCertificateFileMetadata(2018, 6, Csv, C79Certificate, None), ""),
      VatCertificateFile("name_01", "download_url_01", 1300000L, VatCertificateFileMetadata(2018, 6, Pdf, C79Certificate, None), "")
    )

    val postponedVatCertificateFiles = List(
      PostponedVatCertificateFile("name_04", "download_url_06", 111L, PostponedVatCertificateFileMetadata(2018, 3, Pdf, PostponedVATStatement, CDS), ""),
      PostponedVatCertificateFile("name_04", "download_url_05", 111L, PostponedVatCertificateFileMetadata(2018, 4, Csv, PostponedVATStatement, CDS), ""),
      PostponedVatCertificateFile("name_04", "download_url_04", 111L, PostponedVatCertificateFileMetadata(2018, 4, Pdf, PostponedVATStatement, CDS), ""),
      PostponedVatCertificateFile("name_03", "download_url_03", 111L, PostponedVatCertificateFileMetadata(2018, 5, Pdf, PostponedVATStatement, CDS), ""),
      PostponedVatCertificateFile("name_02", "download_url_02", 111L, PostponedVatCertificateFileMetadata(2018, 6, Csv, PostponedVATStatement, CDS), ""),
      PostponedVatCertificateFile("name_01", "download_url_01", 1300000L, PostponedVatCertificateFileMetadata(2018, 6, Pdf, PostponedVATStatement, CDS), "")
    )

    val vatCertificateFilesSdesResponse = List(
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "C79Certificate")))),
      domain.FileInformation("name_04", "download_url_05", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "4"), MetadataItem("FileType", "CSV"), MetadataItem("FileRole", "C79Certificate")))),
      domain.FileInformation("name_04", "download_url_04", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "4"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "C79Certificate")))),
      domain.FileInformation("name_03", "download_url_03", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "5"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "C79Certificate")))),
      domain.FileInformation("name_02", "download_url_02", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "csv"), MetadataItem("FileRole", "C79Certificate")))),
      domain.FileInformation("name_01", "download_url_01", 1300000L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "PDF"), MetadataItem("FileRole", "C79Certificate"))))
    )


    val postponedVatCertificateFilesSdesResponse = List(
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))),
      domain.FileInformation("name_04", "download_url_05", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "4"), MetadataItem("FileType", "CSV"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))),
      domain.FileInformation("name_04", "download_url_04", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "4"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))),
      domain.FileInformation("name_03", "download_url_03", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "5"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))),
      domain.FileInformation("name_02", "download_url_02", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "csv"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))),
      domain.FileInformation("name_01", "download_url_01", 1300000L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "PDF"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate"))))
    )

    val vatCertificateFilesWithUnknownFileTypesSdesResponse = List(
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("FileType", "foo"), MetadataItem("FileRole", "C79Certificate"))))) ++
      vatCertificateFilesSdesResponse ++
      List(domain.FileInformation("name_01", "download_url_01", 1300000L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "bar"), MetadataItem("FileRole", "C79Certificate")))))

    val postponedVatCertificateFilesWithUnknownFileTypesSdesResponse = List(
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("FileType", "foo"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate"))))) ++
      postponedVatCertificateFilesSdesResponse ++
      List(domain.FileInformation("name_01", "download_url_01", 1300000L, Metadata(List(MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "6"), MetadataItem("FileType", "bar"), MetadataItem("FileRole", "PostponedVATStatement"), MetadataItem("DutyPaymentMethod", "Immediate")))))

    val dutyDefermentStatementFiles = List(
      DutyDefermentStatementFile("name_04", "download_url_06", 111L, DutyDefermentStatementFileMetadata(2018, 3, 14, 2018, 3, 23, Pdf, DutyDefermentStatement, Weekly, Some(false), Some("BACS"), someDan, None)),
      DutyDefermentStatementFile("name_04", "download_url_06", 111L, DutyDefermentStatementFileMetadata(2018, 3, 14, 2018, 3, 23, Csv, DutyDefermentStatement, Weekly, Some(false), Some("BACS"), someDan, None))
    )

    val dutyDefermentStatementFilesSdesResponse = List(
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(
        MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
        MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "PDF"), MetadataItem("FileRole", "DutyDefermentStatement"), MetadataItem("DefermentStatementType", "Weekly"), MetadataItem("DutyOverLimit", "N"), MetadataItem("DutyPaymentType", "BACS"), MetadataItem("DAN", someDan)))),
      domain.FileInformation("name_04", "download_url_06", 111L, Metadata(List(
        MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
        MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "CSV"), MetadataItem("FileRole", "DutyDefermentStatement"), MetadataItem("DefermentStatementType", "Weekly"), MetadataItem("DutyOverLimit", "N"), MetadataItem("DutyPaymentType", "BACS"), MetadataItem("DAN", someDan))))
    )


    val securityStatementFiles = List(
      SecurityStatementFile("name_01", "download_url_01", 111L, SecurityStatementFileMetadata(2018, 3, 14, 2018, 3, 23, Csv, SecurityStatement, someEori, 111L, "checksum_01", None)),
      SecurityStatementFile("name_01", "download_url_01", 111L, SecurityStatementFileMetadata(2018, 3, 14, 2018, 3, 23, Pdf, SecurityStatement, someEori, 111L, "checksum_01", None))
    )

    val securityStatementFilesSdesResponse = List(
      domain.FileInformation("name_01", "download_url_01", 111L, Metadata(List(
        MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
        MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "CSV"), MetadataItem("FileRole", "SecurityStatement"),
        MetadataItem("eoriNumber", someEori), MetadataItem("fileSize", "111"), MetadataItem("checksum", "checksum_01"), MetadataItem("issueDate", "3/4/2018")))),
      domain.FileInformation("name_01", "download_url_01", 111L, Metadata(List(
        MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
        MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "pdf"), MetadataItem("FileRole", "SecurityStatement"),
        MetadataItem("eoriNumber", someEori), MetadataItem("fileSize", "111"), MetadataItem("checksum", "checksum_01"), MetadataItem("issueDate", "3/4/2018"))))
    )

    val securityStatementFilesWithUnkownFileTypesSdesResponse =
      List(domain.FileInformation("name_01", "download_url_01", 111L, Metadata(List(
        MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
        MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "foo"), MetadataItem("FileRole", "SecurityStatement"),
        MetadataItem("eoriNumber", someEori), MetadataItem("fileSize", "111"), MetadataItem("checksum", "checksum_01"), MetadataItem("issueDate", "3/4/2018"))))) ++
        securityStatementFilesSdesResponse ++
        List(domain.FileInformation("name_01", "download_url_01", 111L, Metadata(List(
          MetadataItem("PeriodStartYear", "2018"), MetadataItem("PeriodStartMonth", "3"), MetadataItem("PeriodStartDay", "14"), MetadataItem("PeriodEndYear", "2018"),
          MetadataItem("PeriodEndMonth", "3"), MetadataItem("PeriodEndDay", "23"), MetadataItem("FileType", "bar"), MetadataItem("FileRole", "SecurityStatement"),
          MetadataItem("eoriNumber", someEori), MetadataItem("fileSize", "111"), MetadataItem("checksum", "checksum_01"), MetadataItem("issueDate", "3/4/2018")))))

    val sdesGatekeeperServiceSpy = spy(new SdesGatekeeperService())
    val mockHttp = mock[HttpClient]
    val mockAppConfig = mock[AppConfig]
    val mockMetricsReporterService = mock[MetricsReporterService]
    val mockAuditingService = mock[AuditingService]
  }
  //scalastyle:on magic.number

  "HttpSdesService" should {

    "getDutyDefermentStatements" should {
      "have sdesDutyDefermentStatementListUrl configured in AppConfig" in new Setup {
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AppConfig].toInstance(mockAppConfig)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        sdesService.sdesDutyDefermentStatementListUrl
        verify(mockAppConfig).sdesApi
      }

      "make a GET request to sdesDutyDefermentStatementsUrl" in new Setup {
        val url = sdesDutyDefermentStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url), any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))
        await(sdesService.getDutyDefermentStatements(someEori, someDan)(hc))
        verify(mockHttp).GET(eqTo(url), any, any)(any,any,any)
      }

      "filter out unknown file types" in new Setup {
        val url = sdesDutyDefermentStatementsUrl

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(dutyDefermentStatementFilesSdesResponse).toString())))

        val result = await(sdesService.getDutyDefermentStatements(someEoriWithUnknownFileTypes, someDan)(hc))
        verify(mockHttp).GET(eqTo(url),any, any)(any, any, any)
        result must be(dutyDefermentStatementFiles)
      }

      "log response time metric" in new Setup {
        val url = sdesDutyDefermentStatementsUrl
        val cdsAccount = CDSAccounts(newUser().eori, List.empty)

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        when[Future[Seq[CDSAccounts]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
          .thenReturn(Future.successful(Seq(cdsAccount)))
        await(sdesService.getDutyDefermentStatements(someEori, someDan)(hc))
        verify(mockHttp).GET(eqTo(url),any, any)(any, any, any)
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("sdes.get.duty-deferment-statements"))(any)(any)
      }

      "converts Sdes response to List[DutyDefermentStatementFile]" in new Setup {
        val url = sdesDutyDefermentStatementsUrl
        val numberOfStatements = dutyDefermentStatementFilesSdesResponse.length

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(dutyDefermentStatementFilesSdesResponse).toString())))

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when(sdesGatekeeperServiceSpy.convertTo(any)).thenCallRealMethod()
        await(sdesService.getDutyDefermentStatements(someEori, someDan)(hc))
        verify(mockHttp).GET(eqTo(url),any, any)(any, any, any)
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToDutyDefermentStatementFile(any)
      }

      "audit the request" in new Setup {
        val url = sdesDutyDefermentStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AuditingService].toInstance(mockAuditingService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getDutyDefermentStatements(someEori, someDan)(hc))

        import sdesService._
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
        verify(mockAuditingService).audit(eqTo(AuditModel(AUDIT_TYPE, AUDIT_DUTY_DEFERMENT_TRANSACTION, Json.toJson(AuditEori(someEori, false)))))(any, any)
      }
    }

    "getSecurityStatements" should {
      "have sdesSecurityStatementListUrl configured in AppConfig" in new Setup {
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AppConfig].toInstance(mockAppConfig)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        sdesService.sdesSecurityStatementListUrl
        verify(mockAppConfig).sdesApi
      }

      "make a GET request to sdesSecurityStatementsUrl" in new Setup {
        val url = sdesSecurityStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getSecurityStatements(someEori)(hc))
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "filter out unknown file types" in new Setup {
        val url = sdesSecurityStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(securityStatementFilesWithUnkownFileTypesSdesResponse).toString())))

        val result = await(sdesService.getSecurityStatements(someEoriWithUnknownFileTypes)(hc))
        result must be(securityStatementFiles)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "log response time metric" in new Setup {
        val url = sdesSecurityStatementsUrl
        val cdsAccount = CDSAccounts(newUser().eori, List.empty)

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))
        when[Future[Seq[CDSAccounts]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
          .thenReturn(Future.successful(Seq(cdsAccount)))

        await(sdesService.getSecurityStatements(someEori)(hc))
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("sdes.get.security-statements"))(any)(any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "converts Sdes response to List[SecurityStatementFile]" in new Setup {
        val url = sdesSecurityStatementsUrl
        val numberOfStatements = securityStatementFilesSdesResponse.length
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(securityStatementFilesSdesResponse).toString())))

        when(sdesGatekeeperServiceSpy.convertTo(any)).thenCallRealMethod()

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        await(sdesService.getSecurityStatements(someEori)(hc))
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToSecurityStatementFile(any)
      }

      "audit the request" in new Setup {
        val url = sdesSecurityStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AuditingService].toInstance(mockAuditingService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getSecurityStatements(someEori)(hc))

        import sdesService._
        verify(mockAuditingService).audit(eqTo(AuditModel(AUDIT_SECURITY_STATEMENTS, AUDIT_SECURITY_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(someEori, false)))))(any, any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }
    }

    "getVatCertificates" should {
      "have sdesImportVatCertificateListUrl configured in AppConfig" in new Setup {
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AppConfig].toInstance(mockAppConfig)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        sdesService.sdesImportVatCertificateListUrl
        verify(mockAppConfig).sdesApi
      }

      "make a GET request to sdesVatCertificatesUrl" in new Setup {
        val url = sdesVatCertificatesUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse.apply(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getVatCertificates(someEori)(hc,messages))

        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "filter out unknown file types" in new Setup {
        val url = sdesVatCertificatesUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(vatCertificateFilesWithUnknownFileTypesSdesResponse).toString())))

        val result = await(sdesService.getVatCertificates(someEoriWithUnknownFileTypes)(hc,messages))
        result must be(vatCertificateFiles)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "log response time metric" in new Setup {
        val url = sdesVatCertificatesUrl
        val cdsAccount = CDSAccounts(newUser().eori, List.empty)
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))
        when[Future[Seq[CDSAccounts]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
          .thenReturn(Future.successful(Seq(cdsAccount)))

        await(sdesService.getVatCertificates(someEori)(hc,messages))
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("sdes.get.import-vat-certificates"))(any)(any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "converts Sdes response to List[VatCertificateFile]" in new Setup {
        val url = sdesVatCertificatesUrl
        val numberOfStatements = vatCertificateFilesSdesResponse.length
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(vatCertificateFilesSdesResponse).toString())))

        when(sdesGatekeeperServiceSpy.convertTo(any)).thenCallRealMethod()

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        await(sdesService.getVatCertificates(someEori)(hc,messages))
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToVatCertificateFile(any)(any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "audit the request" in new Setup {
        val url = sdesVatCertificatesUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AuditingService].toInstance(mockAuditingService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getVatCertificates(someEori)(hc,messages))

        import sdesService._
        verify(mockAuditingService).audit(eqTo(AuditModel(AUDIT_VAT_CERTIFICATES, AUDIT_VAT_CERTIFICATES_TRANSACTION, Json.toJson(AuditEori(someEori, isHistoric = false)))))(any, any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }
    }

    "getPostponedVatStatements" should {
      "have sdesImportPVatCertificateListUrl configured in AppConfig" in new Setup {
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AppConfig].toInstance(mockAppConfig)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        sdesService.sdesImportPVatCertificateListUrl
        verify(mockAppConfig).sdesApi
      }

      "make a GET request to sdesPostponedVatStatementsUrl" in new Setup {
        val url = sdesPostponedVatStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getPostponedVatStatements(someEori)(hc))

        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "filter out unknown file types" in new Setup {
        val url = sdesPostponedVatStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(postponedVatCertificateFilesWithUnknownFileTypesSdesResponse).toString())))

        val result = await(sdesService.getPostponedVatStatements(someEoriWithUnknownFileTypes)(hc))
        result must be(postponedVatCertificateFiles)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "log response time metric" in new Setup {
        val url = sdesPostponedVatStatementsUrl
        val cdsAccount = CDSAccounts(newUser().eori, List.empty)
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[MetricsReporterService].toInstance(mockMetricsReporterService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]

        when[Future[Seq[CDSAccounts]]](mockMetricsReporterService.withResponseTimeLogging(any)(any)(any))
          .thenReturn(Future.successful(Seq(cdsAccount)))
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getPostponedVatStatements(someEori)(hc))
        verify(mockMetricsReporterService).withResponseTimeLogging(eqTo("sdes.get.postponed-vat-statements"))(any)(any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

      "converts Sdes response to List[PostponedVatCertificateFile]" in new Setup {
        val url = sdesPostponedVatStatementsUrl
        val numberOfStatements = postponedVatCertificateFilesSdesResponse.length
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any, any, any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, Json.toJson(postponedVatCertificateFilesSdesResponse).toString())))

        when(sdesGatekeeperServiceSpy.convertTo(any)).thenCallRealMethod()

        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[SdesGatekeeperService].toInstance(sdesGatekeeperServiceSpy)
        ).build()

        val sdesService = app.injector.instanceOf[SdesService]

        await(sdesService.getPostponedVatStatements(someEori)(hc))
        verify(sdesGatekeeperServiceSpy, times(numberOfStatements)).convertToPostponedVatCertificateFile(any)
        verify(mockHttp).GET(eqTo(url),any, any)(any, any, any)
      }

      "audit the request" in new Setup {
        val url = sdesPostponedVatStatementsUrl
        val app = application().overrides(
          bind[HttpClient].toInstance(mockHttp),
          bind[AuditingService].toInstance(mockAuditingService)
        ).build()
        val sdesService = app.injector.instanceOf[SdesService]
        when[Future[HttpResponse]](mockHttp.GET(eqTo(url),any, any)(any,any,any))
          .thenReturn(Future.successful(HttpResponse(Status.OK, JsArray(Nil).toString())))

        await(sdesService.getPostponedVatStatements(someEori)(hc))

        import sdesService._
        verify(mockAuditingService).audit(eqTo(AuditModel("DisplayPostponedVATStatements", AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(someEori, false)))))(any,any)
        verify(mockHttp).GET(eqTo(url),any, any)(any,any,any)
      }

    }

  }
}
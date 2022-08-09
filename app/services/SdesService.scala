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

import config.AppConfig
import domain.FileFormat._
import domain.{AuditModel, FileInformation, SdesFile, _}
import play.api.i18n.Messages
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesService @Inject()(http: HttpClient,
                            metricsReporter: MetricsReporterService,
                            sdesGatekeeperService: SdesGatekeeperService,
                            auditingService: AuditingService)(implicit appConfig: AppConfig, ec: ExecutionContext) {

  def sdesDutyDefermentStatementListUrl: String = appConfig.sdesApi + "/files-available/list/DutyDefermentStatement"

  def sdesImportVatCertificateListUrl: String = appConfig.sdesApi + "/files-available/list/C79Certificate"

  def sdesImportPVatCertificateListUrl: String = appConfig.sdesApi + "/files-available/list/PostponedVATStatement"

  def sdesSecurityStatementListUrl: String = appConfig.sdesApi + "/files-available/list/SecurityStatement"

  def sdesCsvStatementListUrl: String = appConfig.sdesApi + "/files-available/list/StandingAuthority"

  val AUDIT_DUTY_DEFERMENT_TRANSACTION = "DUTYDEFERMENTSTATEMENTS"
  val AUDIT_VAT_CERTIFICATES_TRANSACTION = "Display VAT certificates"
  val AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION = "Display postponed VAT statements"
  val AUDIT_SECURITY_STATEMENTS_TRANSACTION = "Display security statements"
  val AUDIT_TYPE = "SDESCALL"
  val AUDIT_VAT_CERTIFICATES = "DisplayVATCertificates"
  val AUDIT_SECURITY_STATEMENTS = "DisplaySecurityStatements"
  val AUDIT_POSTPONED_VAT_STATEMENTS = "DisplayPostponedVATStatements"
  val DOWNLOAD_STANDING_AUTHORITIES_NAME = "Download Standing Authorities CSV"
  val DOWNLOAD_STANDING_AUTHORITIES_TYPE = "DownloadStandingAuthoritiesCSV"

  import sdesGatekeeperService._

  def getVatCertificates(eori: String)(implicit hc: HeaderCarrier, messages: Messages): Future[Seq[VatCertificateFile]] = {
    val transform = convertTo[VatCertificateFile] andThen filterFileFormats(SdesFileFormats)

    auditingService.audit(AuditModel(AUDIT_VAT_CERTIFICATES,
      AUDIT_VAT_CERTIFICATES_TRANSACTION, Json.toJson(AuditEori(eori, false))))

    getSdesFiles[FileInformation, VatCertificateFile](
      sdesImportVatCertificateListUrl, eori, "sdes.get.import-vat-certificates", transform)
  }

  def getPostponedVatStatements(eori: String)(implicit hc: HeaderCarrier): Future[Seq[PostponedVatStatementFile]] = {
    val transform = convertTo[PostponedVatStatementFile] andThen filterFileFormats(SdesFileFormats)

    auditingService.audit(AuditModel(AUDIT_POSTPONED_VAT_STATEMENTS,
      AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(eori, false))))

    getSdesFiles[FileInformation, PostponedVatStatementFile](
      sdesImportPVatCertificateListUrl, eori, "sdes.get.postponed-vat-statements", transform)
  }

  def getSecurityStatements(eori: String)(implicit hc: HeaderCarrier): Future[Seq[SecurityStatementFile]] = {
    val transform = convertTo[SecurityStatementFile] andThen filterFileFormats(SdesFileFormats)

    auditingService.audit(AuditModel(AUDIT_SECURITY_STATEMENTS,
      AUDIT_SECURITY_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(eori, false))))

    getSdesFiles[FileInformation, SecurityStatementFile](
      sdesSecurityStatementListUrl, eori, "sdes.get.security-statements", transform)
  }

  def getCsvStatements(eori: String)(implicit hc: HeaderCarrier): Future[Seq[StandingAuthorityFile]] = {
    val transform = convertTo[StandingAuthorityFile] andThen filterFileFormats(authorityFileFormats)

    auditingService.audit(AuditModel(DOWNLOAD_STANDING_AUTHORITIES_NAME,
      DOWNLOAD_STANDING_AUTHORITIES_TYPE, Json.toJson(eori, false)))

    getSdesFiles[FileInformation, StandingAuthorityFile](
      sdesCsvStatementListUrl, eori, "sdes.get.csv-statement", transform)
  }

  def getSdesFiles[A, B <: SdesFile](url: String, key: String, metricsName: String, transform: Seq[A] => Seq[B])
                                    (implicit reads: HttpReads[HttpResponse], readSeq: HttpReads[Seq[A]]): Future[Seq[B]] = {
    metricsReporter.withResponseTimeLogging(metricsName) {
        http.GET[HttpResponse](url, headers = Seq("x-client-id" -> appConfig.xClientIdHeader, "X-SDES-Key" -> key))(reads, HeaderCarrier(), ec)
          .map(readSeq.read("GET", url, _))
          .map(transform)
    }
  }
}

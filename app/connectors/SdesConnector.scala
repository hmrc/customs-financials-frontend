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
import domain.FileFormat.{authorityFileFormats, filterFileFormats}
import domain.FileRole.StandingAuthority
import domain.{FileInformation, SdesFile, StandingAuthorityFile}
import services.{AuditingService, MetricsReporterService, SdesGatekeeperService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads, HttpResponse}
import javax.inject.Inject
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class SdesConnector @Inject()(httpClient: HttpClient,
                              appConfig: AppConfig,
                              metricsReporterService: MetricsReporterService,
                              sdesGatekeeperService: SdesGatekeeperService,
                              auditingService: AuditingService
                             )(implicit executionContext: ExecutionContext) {

  import sdesGatekeeperService._

  def getAuthoritiesCsvFiles(eori: String)(implicit hc: HeaderCarrier): Future[Seq[StandingAuthorityFile]] = {
    val transform = convertTo[StandingAuthorityFile] andThen filterFileFormats(authorityFileFormats)

    getSdesFiles[FileInformation, StandingAuthorityFile](
      appConfig.filesUrl(StandingAuthority),
      eori,
      "sdes.get.csv-statement",
      transform
    )
  }

  def getSdesFiles[A, B <: SdesFile](url: String,
                                     key: String,
                                     metricsName: String,
                                     transform: Seq[A] => Seq[B])
                                    (implicit reads: HttpReads[HttpResponse],
                                     readSeq: HttpReads[Seq[A]],
                                     hc: HeaderCarrier): Future[Seq[B]] = {

    metricsReporterService.withResponseTimeLogging(metricsName) {
      httpClient.GET[HttpResponse](url, headers = Seq("x-client-id" -> appConfig.xClientIdHeader,
          "X-SDES-Key" -> key))(reads, HeaderCarrier(), implicitly)
        .map(readSeq.read("GET", url, _))
        .map(transform)
        .map { files =>
          auditingService.auditFiles(files, key)
          files
        }
    }
  }
}

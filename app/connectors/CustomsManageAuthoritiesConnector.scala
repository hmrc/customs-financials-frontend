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
import domain.EORI
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.mvc.Results
import play.api.mvc.Results.{InternalServerError, Ok}
import services.MetricsReporterService
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomsManageAuthoritiesConnector @Inject()(httpClient: HttpClient,
                                                  appConfig: AppConfig,
                                                  metricsReporter: MetricsReporterService)
                                                 (implicit executionContext: ExecutionContext) extends Logging {

  def fetchAndSaveAccountAuthoritiesInCache(eori: EORI)(implicit hc: HeaderCarrier): Future[Results.Status] = {
    val endPointUrl = s"${appConfig.manageAuthoritiesServiceUrl}/account-authorities/fetch-authorities/$eori"

    httpClient.GET[HttpResponse](endPointUrl).map {
      res =>
        res.status match {
          case OK =>
            logger.info(s"Authorities' details have been successfully saved in the cache for $eori")
            Future.successful(Ok)

          case NO_CONTENT =>
            logger.info(s"No data found for $eori")
            Future.successful(Ok)

          case INTERNAL_SERVER_ERROR =>
            logger.warn(s"Error occurred while saving the authorities' details in cache for $eori")
            Future.successful(InternalServerError)
        }
    }.recover {
      case _ =>
        logger.warn(s"Error occurred while saving the authorities' details in cache for $eori")
        Future.successful(InternalServerError)
    }.flatten

  }
}

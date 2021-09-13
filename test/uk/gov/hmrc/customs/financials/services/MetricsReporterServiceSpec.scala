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

import com.codahale.metrics.{Histogram, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers
import play.api.{Application, inject}
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http._

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MetricsReporterServiceSpec extends SpecBase {




  "MetricsReporterService" should {

    "withResponseTimeLogging" should {

        "log successful call metrics" in new Setup{

          running(app){
            await {
              service.withResponseTimeLogging("foo") {
                Future.successful("OK")
              }
            }
            verify(mockRegistry).histogram("responseTimes.foo.200")
            verify(mockHistogram).update(elapsedTimeInMillis)
          }
        }

        "log default error during call metrics" in new Setup{
          running(app){
            assertThrows[InternalServerException] {
              await {
                service.withResponseTimeLogging("bar") {
                  Future.failed(new InternalServerException("boom"))
                }
              }
            }
            verify(mockRegistry).histogram("responseTimes.bar.500")
            verify(mockHistogram).update(elapsedTimeInMillis)
          }

        }

        "log not found call metrics" in new Setup{
          running(app){
            assertThrows[NotFoundException] {
              await {
                service.withResponseTimeLogging("bar") {
                  Future.failed(new NotFoundException("boom"))
                }
              }
            }
            verify(mockRegistry).histogram("responseTimes.bar.404")
            verify(mockHistogram).update(elapsedTimeInMillis)
          }
        }

        "log bad request error call metrics" in new Setup{
          running(app){
            assertThrows[BadRequestException] {
              await {
                service.withResponseTimeLogging("bar") {
                  Future.failed(new BadRequestException("boom"))
                }
              }
            }
            verify(mockRegistry).histogram("responseTimes.bar.400")
            verify(mockHistogram).update(elapsedTimeInMillis)
          }
        }

        "log 5xx error call metrics" in new Setup{
          running(app){
            assertThrows[UpstreamErrorResponse] {
              await {
                service.withResponseTimeLogging("bar") {
                  Future.failed(UpstreamErrorResponse("boom", Status.SERVICE_UNAVAILABLE, Status.NOT_IMPLEMENTED))
                }
              }
            }
            verify(mockRegistry).histogram("responseTimes.bar.503")
            verify(mockHistogram).update(elapsedTimeInMillis)
          }
        }

        "log 4xx error call metrics" in new Setup{
          assertThrows[UpstreamErrorResponse] {
            await {
              service.withResponseTimeLogging("bar") {
                Future.failed(UpstreamErrorResponse("boom", Status.FORBIDDEN, Status.NOT_IMPLEMENTED))
              }
            }
          }
          verify(mockRegistry).histogram("responseTimes.bar.403")
          verify(mockHistogram).update(elapsedTimeInMillis)
        }
    }
  }

  trait Setup {
    val mockDateTimeService = mock[DateTimeService]
    val startTimestamp = OffsetDateTime.parse("2018-11-09T17:15:30+01:00")
    val endTimestamp = OffsetDateTime.parse("2018-11-09T17:15:35+01:00")
    val elapsedTimeInMillis = 5000L // endTimestamp - startTimestamp
    when(mockDateTimeService.getTimeStamp())
      .thenReturn(startTimestamp,endTimestamp)

    val mockHistogram = mock[Histogram]

    val mockRegistry = mock[MetricRegistry]
    when(mockRegistry.histogram(ArgumentMatchers.any())).thenReturn(mockHistogram)

    val mockMetrics = mock[Metrics]
    when(mockMetrics.defaultRegistry).thenReturn(mockRegistry)

    val app: Application = GuiceApplicationBuilder().overrides(
      inject.bind[DateTimeService].toInstance(mockDateTimeService),
      inject.bind[Histogram].toInstance(mockHistogram),
      inject.bind[Metrics].toInstance(mockMetrics)
    ).configure(
      "microservice.metrics.enabled" -> false,
      "metrics.enabled" -> false,
      "auditing.enabled" -> false
    ).build()

    val service: MetricsReporterService = app.injector.instanceOf[MetricsReporterService]
  }
}

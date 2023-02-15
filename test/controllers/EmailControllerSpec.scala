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

package controllers

import connectors.CustomsFinancialsApiConnector
import domain.EmailUnverifiedResponse
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.inject.bind
import play.api.test.Helpers._
import services.MetricsReporterService
import uk.gov.hmrc.http.HeaderCarrier
import utils.SpecBase

import scala.concurrent.Future


class EmailControllerSpec extends SpecBase {


 /* "The Verify Your Email page" should {
    "Redirect users with unverified emails" in new Setup {

      running(app) {
        when(mockCustomsFinancialsApiConnector.isEmailUnverified(hc)).thenReturn(Future.successful("someUnverified@email.com"))
        val request = fakeRequest(GET, routes.EmailController.showUnverified.url)
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))
        status(result) mustBe NOT_FOUND
        html.containsLinkWithText("/manage-email-cds/service/customs-finance", "Verify or change your email address") mustBe false
      }
    }
  }

  trait Setup {

    val mockCustomsFinancialsApiConnector: CustomsFinancialsApiConnector = mock[CustomsFinancialsApiConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val expectedResult = EmailUnverifiedResponse(Some("unVerifiedEmail"))
    val mockMetricsReporterService: MetricsReporterService = mock[MetricsReporterService]

    val app = application().overrides(
      bind[MetricsReporterService].toInstance(mockMetricsReporterService),
      inject.bind[CustomsFinancialsApiConnector].toInstance(mockCustomsFinancialsApiConnector)
    ).build()
  }*/
}

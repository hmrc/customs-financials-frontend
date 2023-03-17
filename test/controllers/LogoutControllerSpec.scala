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

import connectors.CustomsFinancialsSessionCacheConnector
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import utils.SpecBase

import scala.concurrent.Future

class LogoutControllerSpec extends SpecBase {

 /* "LogoutController logout" should {
    "redirect to feedback survey page" in new Setup {
      val request = fakeRequest(GET, routes.LogoutController.logout.url).withHeaders("X-Session-Id" -> "someSession")

      when(mockSessionCacheConnector.removeSession(eqTo("someSession"))(any)).thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      running(app) {
        val result = route(app, request).value
        redirectLocation(result).value mustBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=https%3A%2F%2Fwww.development.tax.service.gov.uk%2Ffeedback%2FCDS-FIN"
      }
    }
  }

  "LogoutController logout no survey" should {
    "redirect to sign-out with the continue as the financials homepage" in new Setup {
      when(mockSessionCacheConnector.removeSession(eqTo("someSession"))(any)).thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

      running(app) {
        val request = fakeRequest(GET, routes.LogoutController.logoutNoSurvey.url).withHeaders("X-Session-Id" -> "someSession")
        val result = route(app, request).value
        redirectLocation(result).value mustBe "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http%3A%2F%2Flocalhost%3A9876%2Fcustoms%2Fpayment-records"
      }
    }
  }

  trait Setup {
    val mockAuthConnector = mock[AuthConnector]
    val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]
    val app = application().overrides(
      inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector),
      inject.bind[AuthConnector].toInstance(mockAuthConnector)
    ).build()
  }*/
}

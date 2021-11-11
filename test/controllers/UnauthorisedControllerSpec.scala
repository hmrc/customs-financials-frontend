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

package controllers

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.inject
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders, SessionRecordNotFound}
import utils.SpecBase
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class UnauthorisedControllerSpec extends SpecBase {

  "UnauthorisedController" should {
    "load 'not subscribed to cds' page" in new Setup {
      when(mockAuthConnector.authorise(meq(AuthProviders(GovernmentGateway)), meq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful({}))

      running(app){
        val request = fakeRequest(GET, routes.UnauthorisedController.onPageLoad().url).withHeaders("X-Session-Id" -> "someSession")
        val result = route(app, request).value
        status(result) mustBe OK
        val html = Jsoup.parse(contentAsString(result))
        html.getElementsByTag("h1").text mustBe "To continue with this you need to get access to Customs Declaration Services (CDS)"
      }
    }

    "not load 'not subscribed to cds' page" when {
      "user is not authorised with GG"  in new Setup {
        when(mockAuthConnector.authorise(meq(AuthProviders(GovernmentGateway)), meq(EmptyRetrieval))(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(SessionRecordNotFound()))

        running(app){
          val request = fakeRequest(GET, routes.UnauthorisedController.onPageLoad().url).withHeaders("X-Session-Id" -> "someSession")
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9876%2Fcustoms%2Fpayment-records"
        }
      }
    }
  }

  trait Setup {
    val mockAuthConnector = mock[AuthConnector]
    val app = application().overrides(
      inject.bind[AuthConnector].toInstance(mockAuthConnector)
    ).build()
  }
}

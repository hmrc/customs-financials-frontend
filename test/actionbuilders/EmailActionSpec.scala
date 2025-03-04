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

package actionbuilders

import domain.{UndeliverableEmail, UnverifiedEmail}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.{Application, inject}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.DataStoreService
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.ServiceUnavailableException
import utils.SpecBase
import utils.MustMatchers

import scala.concurrent.Future

class EmailActionSpec extends SpecBase with MustMatchers {

  "EmailAction" should {
    "Let requests with validated email through" in new Setup {
      running(app) {
        when(mockDataStoreService.getEmail(any))
          .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))

        val response = await(emailAction.filter(authenticatedRequest))
        response mustBe None
      }
    }

    "Display undeliverable page when getEmail returns undeliverable" in new Setup {
      when(mockDataStoreService.getEmail(any))
        .thenReturn(Future.successful(Left(UndeliverableEmail("some@email.com"))))

      val response: Option[Result] = await(emailAction.filter(authenticatedRequest))

      response.get.header.status mustBe SEE_OTHER
      response.get.header.headers(LOCATION) must include("/undeliverable-email")
    }

    "Let request through, when getEmail throws service unavailable exception" in new Setup {
      running(app) {
        when(mockDataStoreService.getEmail(any))
          .thenReturn(Future.failed(new ServiceUnavailableException(emptyString)))

        val response = await(emailAction.filter(authenticatedRequest))
        response mustBe None
      }
    }

    "Redirect users with unvalidated emails" in new Setup {
      running(app) {
        when(mockDataStoreService.getEmail(any)).thenReturn(Future.successful(Left(UnverifiedEmail)))

        val response = await(emailAction.filter(authenticatedRequest))
        response.get.header.status mustBe SEE_OTHER
        response.get.header.headers(LOCATION) must include("/verify-your-email")
      }
    }
  }

  trait Setup {
    val mockDataStoreService: DataStoreService = mock[DataStoreService]

    val app: Application = application()
      .overrides(
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      )
      .build()

    val emailAction: EmailAction                                           = app.injector.instanceOf[EmailAction]
    val authenticatedRequest: AuthenticatedRequest[AnyContentAsEmpty.type] =
      AuthenticatedRequest(FakeRequest("GET", "/"), newUser())
  }
}

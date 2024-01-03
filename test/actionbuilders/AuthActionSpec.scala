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

import com.google.inject.Inject
import config.AppConfig
import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.{Application, inject}
import play.api.mvc.{Action, AnyContent, BodyParsers, Result, Results}
import play.api.test.Helpers._
import services.{AuditingService, DataStoreService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.auth.core.Enrolments
import utils.SpecBase
import controllers.routes

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  "the action" should {

    "redirect to the Government Gateway sign-in page when no authenticated user" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new MissingBearerToken),
        config,
        bodyParsers,
        mockDataStoreService
      )
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(
          fakeRequest().withHeaders("X-Session-Id" -> "someSessionId")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(config.loginUrl)
      }
    }

    "redirect the user to login when the user's session has expired" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new BearerTokenExpired),
        config,
        bodyParsers,
        mockDataStoreService
      )
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(
          fakeRequest().withHeaders("X-Session-Id" -> "someSessionId")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(config.loginUrl)
      }
    }

    "redirect the user to login when the user has an unexpected Auth provider" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new UnsupportedAuthProvider),
        config,
        bodyParsers,
        mockDataStoreService
      )
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(
          fakeRequest().withHeaders("X-Session-Id" -> "someSessionId")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(
          "/customs/payment-records/not-subscribed-for-cds"
        )
      }
    }

    "redirect the user to unauthorised controller when has insufficient enrolments" in new Setup {
      when(
        mockAuthConnector.authorise[Option[Credentials] ~ Option[Name] ~ Option[Email]
          ~ Option[AffinityGroup] ~ Option[String] ~ Enrolments](any, any)(
          any,
          any
        )
      )
        .thenReturn(
          Future.successful(
            Some(Credentials("someProviderId", "someProviderType")) ~
              Some(Name(Some("someName"), Some("someLastName"))) ~
              Some(Email("some@email.com")) ~
              Some(AffinityGroup.Individual) ~
              Some("id") ~
              Enrolments(Set.empty)
          )
        )

      val authAction = new AuthAction(
        mockAuthConnector,
        config,
        bodyParsers,
        mockDataStoreService
      )
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(
          fakeRequest().withHeaders("X-Session-Id" -> "someSessionId")
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(
          "/customs/payment-records/not-subscribed-for-cds"
        )
      }
    }


    "redirect the user to the unauthorised page when no sufficient enrolments" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new InsufficientEnrolments),
        config,
        bodyParsers,
        mockDataStoreService
      )

      val controller = new Harness(authAction)
      val result: Future[Result] = controller.onPageLoad()(fakeRequest())

      running(app) {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "redirect the user to the unauthorised page when no sufficient confidence level" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new InsufficientConfidenceLevel),
        config,
        bodyParsers,
        mockDataStoreService
      )

      val controller = new Harness(authAction)
      val result = controller.onPageLoad()(fakeRequest())

      running(app) {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "redirect the user to the unauthorised page when no unsupported affinity group" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
        config,
        bodyParsers,
        mockDataStoreService
      )

      val controller = new Harness(authAction)
      val result: Future[Result] = controller.onPageLoad()(fakeRequest())

      running(app) {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "redirect the user to the unauthorised page when no unsupported credential role" in new Setup {
      val authAction = new AuthAction(
        new FakeFailingAuthConnector(new UnsupportedCredentialRole),
        config,
        bodyParsers,
        mockDataStoreService
      )

      val controller = new Harness(authAction)
      val result: Future[Result] = controller.onPageLoad()(fakeRequest())

      running(app) {
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  trait Setup {
    val mockAuditingService: AuditingService = mock[AuditingService]
    val mockDataStoreService: DataStoreService = mock[DataStoreService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]

    val app: Application = application()
      .overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()

    val config: AppConfig = app.injector.instanceOf[AppConfig]
    val bodyParsers: BodyParsers.Default = application().injector().instanceOf[BodyParsers.Default]

    class Harness(authAction: IdentifierAction) {
      def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
    }

    implicit class Ops[A](a: A) {
      def ~[B](b: B): A ~ B = new~(a, b)
    }

    class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
      val serviceUrl: String = ""

      override def authorise[A](predicate: Predicate,
                                retrieval: Retrieval[A]
                               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        Future.failed(exceptionToReturn)
    }
  }
}

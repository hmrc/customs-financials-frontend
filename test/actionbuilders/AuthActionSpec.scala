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

package actionbuilders

import actionbuilders.{AuthAction, AuthActionHelper, IdentifierAction}
import com.google.inject.Inject
import play.api.inject
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, _}
import config.AppConfig
import domain.{AuditModel, EoriHistory}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import services.{AuditingService, DataStoreService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.SpecBase

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad() = authAction { _ => Results.Ok }
  }

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  "the action" should {

    "redirect to the Government Gateway sign-in page when no authenticated user" in {
      val mockAuditingService = mock[AuditingService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()
      val config = app.injector.instanceOf[AppConfig]
      val bodyParsers = application().injector.instanceOf[BodyParsers.Default]
      val authActionHelper = app.injector.instanceOf[AuthActionHelper]

      val authAction = new AuthAction(new FakeFailingAuthConnector(new MissingBearerToken), config, bodyParsers, authActionHelper)
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(config.loginUrl)
      }
    }

    "redirect the user to login when the user's session has expired" in {
      val mockAuditingService = mock[AuditingService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()
      val config = app.injector.instanceOf[AppConfig]
      val bodyParsers = application().injector.instanceOf[BodyParsers.Default]
      val authActionHelper = app.injector.instanceOf[AuthActionHelper]

      val authAction = new AuthAction(new FakeFailingAuthConnector(new BearerTokenExpired), config, bodyParsers, authActionHelper)
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith(config.loginUrl)
      }
    }

    "redirect the user to login when the user has an unexpected Auth provider" in {
      val mockAuditingService = mock[AuditingService]
      val mockDataStoreService = mock[DataStoreService]

      val app = application().overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()
      val config = app.injector.instanceOf[AppConfig]
      val bodyParsers = application().injector.instanceOf[BodyParsers.Default]
      val authActionHelper = app.injector.instanceOf[AuthActionHelper]

      val authAction = new AuthAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), config, bodyParsers, authActionHelper)
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith("/customs/payment-records/not-subscribed-for-cds")
      }
    }

    "redirect the user to unauthorised controller when has insufficient enrolments" in {
      val mockAuditingService = mock[AuditingService]
      val mockDataStoreService = mock[DataStoreService]
      val mockAuthConnector = mock[AuthConnector]

      when(mockAuthConnector.authorise[Option[Credentials] ~ Option[Name] ~ Option[Email] ~ Option[AffinityGroup] ~ Option[String] ~ Enrolments](any, any)(any, any))
        .thenReturn(Future.successful(
          Some(Credentials("someProviderId", "someProviderType")) ~
            Some(Name(Some("someName"), Some("someLastName"))) ~
            Some(Email("some@email.com")) ~
            Some(AffinityGroup.Individual) ~
            Some("id") ~
            Enrolments(Set.empty)))

      val app = application().overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()
      val config = app.injector.instanceOf[AppConfig]
      val bodyParsers = application().injector.instanceOf[BodyParsers.Default]
      val authActionHelper = app.injector.instanceOf[AuthActionHelper]

      val authAction = new AuthAction(mockAuthConnector, config, bodyParsers, authActionHelper)
      val controller = new Harness(authAction)

      running(app) {
        val result = controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId"))
        status(result) mustBe SEE_OTHER
        redirectLocation(result).get must startWith("/customs/payment-records/not-subscribed-for-cds")
      }
    }

    "audit the historic EORIs for authenticated users in" in {
      val mockAuditingService = mock[AuditingService]
      val mockDataStoreService = mock[DataStoreService]
      val mockAuthConnector = mock[AuthConnector]

      val validFrom = LocalDate.now().minusDays(30)
      val validTo = LocalDate.now().plusDays(30)

      when(mockDataStoreService.getAllEoriHistory(any)(any))
        .thenReturn(Future.successful(
          Seq(
            EoriHistory("testEori1", validFrom = Some(validFrom), validUntil = Some(validTo)),
            EoriHistory("testEori2", validFrom = Some(validFrom), validUntil = Some(validTo))
          )))

      val expectedAuditEvent = Json.arr(
        Json.obj(
          "eori" -> "testEori3",
          "isHistoric" -> false
        ),
        Json.obj(
          "eori" -> "testEori1",
          "isHistoric" -> true
        ),
        Json.obj(
          "eori" -> "testEori2",
          "isHistoric" -> true
        )
      )

      when(mockAuditingService.audit(eqTo(AuditModel("ViewAccount", "View account", expectedAuditEvent)))(any, any)).thenReturn(Future.successful(AuditResult.Success))

      when(mockAuthConnector.authorise[Option[Credentials] ~ Option[Name] ~ Option[Email] ~ Option[AffinityGroup] ~ Option[String] ~ Enrolments](any, any)(any, any))
        .thenReturn(Future.successful(
          Some(Credentials("someProviderId", "someProviderType")) ~
            Some(Name(Some("someName"), Some("someLastName"))) ~
            Some(Email("some@email.com")) ~
            Some(AffinityGroup.Individual) ~
            Some("id") ~
            Enrolments(Set(Enrolment("HMRC-CUS-ORG", identifiers = Seq(EnrolmentIdentifier("EORINumber", "testEori3")), "Activated")))))

      val app = application().overrides(
        inject.bind[AuditingService].toInstance(mockAuditingService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService)
      ).build()
      val config = app.injector.instanceOf[AppConfig]
      val bodyParsers = application().injector.instanceOf[BodyParsers.Default]
      val authActionHelper = app.injector.instanceOf[AuthActionHelper]

      val authAction = new AuthAction(mockAuthConnector, config, bodyParsers, authActionHelper)
      val controller = new Harness(authAction)
      running(app) {
        val result = await(controller.onPageLoad()(fakeRequest().withHeaders("X-Session-Id" -> "someSessionId")))
        result.header.status mustBe OK
        Thread.sleep(1000)
        verify(mockAuditingService).audit(eqTo(AuditModel("ViewAccount", "View account", expectedAuditEvent)))(any, any)
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
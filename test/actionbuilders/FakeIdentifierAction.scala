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



import domain.{EoriHistory, SignedInUser}
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction @Inject()(bodyParsers: PlayBodyParsers)(eoriHistory: Seq[EoriHistory]) extends IdentifierAction {


  lazy val newUser = {
    val eori = "testEori1"
    SignedInUser(
      Some(Credentials("2345235235", "GovernmentGateway")),
      Some(Name(Some("firstName"), Some("secondName"))),
      Some("test@email.com"),
      "testEori1",
      Some(AffinityGroup.Individual),
      Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
      Enrolments(Set(
        Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "111111111")), "Activated", None),
        Enrolment("IR-CT", List(EnrolmentIdentifier("UTR", "222222222")), "Activated", None),
        Enrolment("HMRC-CUS-ORG", List(EnrolmentIdentifier("EORINumber", eori)), "Activated", None)
      )),
      eoriHistory
    )
  }

  override def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
    Future.successful(Right(AuthenticatedRequest(request, newUser)))

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
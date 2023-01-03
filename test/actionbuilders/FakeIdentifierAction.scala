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

import domain.{EoriHistory, SignedInUser}
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeIdentifierAction @Inject()(bodyParsers: PlayBodyParsers)(eoriHistory: Seq[EoriHistory]) extends IdentifierAction {

  lazy val newUser: SignedInUser = SignedInUser("testEori1", eoriHistory)

  override def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] =
    Future.successful(Right(AuthenticatedRequest(request, newUser)))

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
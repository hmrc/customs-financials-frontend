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

import com.google.inject.Inject
import config.AppConfig
import controllers.routes
import domain.SignedInUser
import play.api.mvc.Results.Redirect
import play.api.mvc._
import services.DataStoreService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionRefiner[Request, AuthenticatedRequest]

class AuthAction @Inject()(val authConnector: AuthConnector,
                           appConfig: AppConfig,
                           val parser: BodyParsers.Default,
                           dataStoreService: DataStoreService)
                          (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.allEnrolments) {
      allEnrolments =>
        allEnrolments.getEnrolment("HMRC-CUS-ORG").flatMap(_.getIdentifier("EORINumber")) match {
          case Some(eori) =>
            for {
              allEoriHistory <- dataStoreService.getAllEoriHistory(eori.value)
              signedInUser = SignedInUser(eori.value, allEoriHistory)
            } yield Right(AuthenticatedRequest(request, signedInUser))
          case None => Future.successful(Left(Redirect(routes.UnauthorisedController.onPageLoad)))
        }
    }
  } recover {
    case _: NoActiveSession =>
      Left(Redirect(appConfig.loginUrl, Map("continue_url" -> Seq(appConfig.loginContinueUrl))))
    case _: InsufficientEnrolments =>
      Left(Redirect(routes.UnauthorisedController.onPageLoad))
    case _ =>
      Left(Redirect(routes.UnauthorisedController.onPageLoad))
  }
}

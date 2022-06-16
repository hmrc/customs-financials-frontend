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

package config

import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import views.html.error_states.{error_template, not_found_template}
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import javax.inject.{Inject, Singleton}

@Singleton
class ErrorHandler @Inject()(val messagesApi: MessagesApi,
                             implicit val appConfig: AppConfig,
                             notFoundView: not_found_template,
                             errorTemplate: error_template
                            ) extends FrontendErrorHandler {
  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    errorTemplate(Messages("cf.error.standard-error.title"), Messages("cf.error.standard-error.heading"),
      Messages("cf.error.standard-error.message"), "", "")

  override def notFoundTemplate(implicit request: Request[_]): Html =
    notFoundView()

  def unauthorized()(implicit request: Request[_]): Html = {
    errorTemplate(Messages("cf.error.unauthorized.title"), Messages("cf.error.unauthorized.heading"),
      Messages("cf.error.unauthorized.message"))
  }

  def technicalDifficulties()(implicit request: Request[_]): Html = {
    errorTemplate(Messages("cf.error.technicalDifficulties.title"), Messages("cf.error.technicalDifficulties.heading"),
      Messages("cf.error.technicalDifficulties.message"))
  }
}

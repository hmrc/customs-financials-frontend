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

package viewmodels

import play.api.i18n.Messages
import play.api.mvc.Request
import config.AppConfig
import domain.{AuthorizedToViewPageState, CDSAccount}

case class AuthorizedToViewViewModel(eori: String,
                                     accounts: Seq[CDSAccount],
                                     pageState: AuthorizedToViewPageState
                                    )(implicit request: Request[_], appConfig: AppConfig, messages: Messages)
  extends Paginated[CDSAccount] {

  val allItems: Seq[CDSAccount] = accounts
  val itemsPerPage: Int = appConfig.numberOfItemsPerPage
  val requestedPage: Int = pageState.page
  val urlForPage: Int => String = pageState.urlForPageFactory(request.path)
  val itemsDescription = messages("cf.account.authorized-to-view.items-description")
}

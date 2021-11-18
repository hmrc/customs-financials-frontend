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

package domain

import play.api.mvc.QueryStringBindable

case class AuthorizedToViewPageState(page: Int) {
  def urlForPageFactory(path: String): Int => String = { pageNumber =>
    val queryParams = AuthorizedToViewPageState.queryStringBinder.unbind("", this.copy(page = pageNumber))
    s"$path?$queryParams"
  }
}

object AuthorizedToViewPageState {

  val pageQueryKey = "page"

  implicit def queryStringBinder(implicit intBinder: QueryStringBindable[Option[Int]]): QueryStringBindable[AuthorizedToViewPageState] = new QueryStringBindable[AuthorizedToViewPageState] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AuthorizedToViewPageState]] = {
      intBinder.bind(pageQueryKey, params).map {
        case Right(maybePage) => Right(AuthorizedToViewPageState(maybePage.getOrElse(1)))
        case _ => Left("Unable to extract page query param")
      }
    }

    override def unbind(key: String, pager: AuthorizedToViewPageState): String = {
      intBinder.unbind(pageQueryKey, Some(pager.page))
    }

  }

}

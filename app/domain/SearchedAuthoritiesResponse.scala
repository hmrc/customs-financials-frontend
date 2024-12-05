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

package domain

import play.api.libs.json.{Json, OFormat}

case class SearchedAuthoritiesResponse(
    numberOfAuthorities: String,
    dutyDefermentAccounts: Option[Seq[AuthorisedDutyDefermentAccount]],
    generalGuaranteeAccounts: Option[Seq[AuthorisedGeneralGuaranteeAccount]],
    cdsCashAccounts: Option[Seq[AuthorisedCashAccount]]
) {
  def toSearchAuthorities: SearchedAuthorities =
    SearchedAuthorities(
      numberOfAuthorities,
      Seq(
        dutyDefermentAccounts.getOrElse(Seq.empty),
        generalGuaranteeAccounts.getOrElse(Seq.empty),
        cdsCashAccounts.getOrElse(Seq.empty)
      ).flatten
    )
}

object SearchedAuthoritiesResponse {
  implicit val format: OFormat[SearchedAuthoritiesResponse] = Json.format[SearchedAuthoritiesResponse]
}

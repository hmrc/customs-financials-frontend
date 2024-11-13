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

import domain.DutyPaymentMethod.CDS
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import play.api.libs.ws.BodyWritable

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.util.Random

case class AccountsRequestCommon(receiptDate: String, acknowledgementReference: String, regime: String)

object AccountsRequestCommon {
  private val MDG_ACK_REF_LENGTH = 32
  private val RANDOM_INT_LENGTH = 10

  def generate: AccountsRequestCommon = {
    val isoLocalDateTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS))
    val acknowledgmentRef = generateStringOfRandomDigits(MDG_ACK_REF_LENGTH)
    val regime = CDS

    AccountsRequestCommon(isoLocalDateTime, acknowledgmentRef, regime)
  }

  private def generateStringOfRandomDigits(length: Int): String = {
    (1 to length).map(_ => Random.nextInt(RANDOM_INT_LENGTH)).mkString
  }

  implicit val format: OFormat[AccountsRequestCommon] = Json.format[AccountsRequestCommon]

  implicit def jsonBodyWritable[T](implicit
                                   writes: Writes[T],
                                   jsValueBodyWritable: BodyWritable[JsValue]
                                  ): BodyWritable[T] = jsValueBodyWritable.map(writes.writes)
}

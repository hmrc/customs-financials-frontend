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

package uk.gov.hmrc.customs.financials.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers._
domain.DDStatementType.Weekly
domain.FileFormat._
domain.FileRole.DutyDefermentStatement
utils.SpecBase

import scala.util.Random

class SdesFileSpec extends SpecBase with Matchers {

  "an SdesFile" should {

    "be correctly ordered" in new Setup() {

      val metadata = randomDutyDefermentStatementFile(1).metadata
      val pdf = randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Pdf))
      val csv = randomDutyDefermentStatementFile(1).copy(metadata = metadata.copy(fileFormat = Csv))

      List(csv,pdf).sorted.map(_.metadata).map(_.fileFormat) must be (List(Pdf, Csv))
      List(pdf,csv).sorted.map(_.metadata).map(_.fileFormat) must be (List(Pdf, Csv))

    }
  }

  trait Setup {

    def randomInt(limit: Int) = Random.nextInt(limit)
    def randomString(length: Int): String = Random.alphanumeric.take(length).mkString

    def randomDutyDefermentStatementFile(size: Long): DutyDefermentStatementFile = DutyDefermentStatementFile(
      s"${randomString(8)}.${randomString(3)}",
      s"http://${randomString(8)}.com/",
      size,
      DutyDefermentStatementFileMetadata(randomInt(2017) + 1, randomInt(11) + 1, randomInt(27) + 1,
        randomInt(2017) + 1, randomInt(11) + 1, randomInt(27) + 1, Pdf, DutyDefermentStatement,
        Weekly, Some(true), Some("BACS"), s"${randomInt(8)}", None)
    )
  }

}

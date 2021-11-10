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

package uk.gov.hmrc.customs.financials.controllers

domain.FileFormat._
domain.FileRole._
domain._
utils.SpecBase
viewmodels.SecurityStatementsViewModel

import java.time.LocalDate

class SecurityStatementsViewModelSpec extends SpecBase {

  private val someEori = "12345678"

  "The SecurityStatementsViewModel" should {
      "indicate that there are no current statements" when {
        "none of the EORIs have any current statements" in {

          val statementsForEori1 = SecurityStatementsForEori(EoriHistory("GB12345", None, None), Seq.empty, Seq.empty)
          val statementsForEori2 = SecurityStatementsForEori(EoriHistory("GB67890", None, None), Seq.empty, Seq.empty)
          val model = SecurityStatementsViewModel(Seq(statementsForEori1, statementsForEori2))
          model.hasCurrentStatements mustBe false
        }
    }

    "indicate that there are current statements" when {
      "some of the EORIs have any current statements" in {
        val files = List(
          SecurityStatementFile("statementfile_00", "download_url_00", 99L, SecurityStatementFileMetadata(2017, 12, 28, 2018, 1, 1, Pdf, SecurityStatement, someEori, 500L, "0000000", None)),
          SecurityStatementFile("statementfile_01", "download_url_01", 111L, SecurityStatementFileMetadata(2018, 1, 2, 2018, 1, 31, Pdf, SecurityStatement, someEori, 1500L, "1123456", None))
        )
        val currentStatements = Seq(SecurityStatementsByPeriod(LocalDate.now(), LocalDate.now(), files))
        val statementsForEori1 = SecurityStatementsForEori(EoriHistory("GB12345", None, None), currentStatements, Seq.empty)
        val statementsForEori2 = SecurityStatementsForEori(EoriHistory("GB67890", None, None), Seq.empty, Seq.empty)
        val model = SecurityStatementsViewModel(Seq(statementsForEori1, statementsForEori2))
        model.hasCurrentStatements mustBe true
      }
    }
  }

}

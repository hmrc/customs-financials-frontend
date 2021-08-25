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

package uk.gov.hmrc.customs.financials.viewmodels

import uk.gov.hmrc.customs.financials.domain.DDStatementType.Weekly
import uk.gov.hmrc.customs.financials.domain.FileFormat._
import uk.gov.hmrc.customs.financials.domain.FileRole._
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.viewmodels.{DutyDefermentAccount => vmDutyDefermentAccount}

import java.time.LocalDate

class DutyDefermentAccountSpec extends SpecBase    {

  "ViewModel" should {
    val someDan = "1234567"
    val someLinkId = "029812878213"
    val someHistoricEori = "GB12345678"
    val someFileSize = 1024L
    val someStatementFiles = Seq(DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None)))

    "be able to provide groups of statement periods by month" in {

      val dutyDefermentStatementFiles = List(
        DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)),
        DutyDefermentStatementFile("2018_04_01-08.csv", "url.csv", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)),
        DutyDefermentStatementFile("2018_04_09-15.pdf", "url2.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)),
        DutyDefermentStatementFile("2018_04_09-15.csv", "url2.csv", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None))
      )

      val modelTest = vmDutyDefermentAccount("123", Seq(DutyDefermentStatementsForEori(EoriHistory("GB123123123", None, None), dutyDefermentStatementFiles, Seq.empty)), "someLinkId")

      val statementPeriodsByMonth = List(DutyDefermentStatementPeriodsByMonth(LocalDate.parse("2018-04-01"), List(
        DutyDefermentStatementPeriod(DutyDefermentStatement, Weekly, LocalDate.parse("2018-04-01"), LocalDate.parse("2018-04-09"), LocalDate.parse("2018-04-15"), List(DutyDefermentStatementFile("2018_04_09-15.pdf", "url2.pdf", 1024, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)),
          DutyDefermentStatementFile("2018_04_09-15.csv", "url2.csv", 1024, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)))),
        DutyDefermentStatementPeriod(DutyDefermentStatement, Weekly, LocalDate.parse("2018-04-01"), LocalDate.parse("2018-04-01"), LocalDate.parse("2018-04-08"), List(DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", 1024, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)),
          DutyDefermentStatementFile("2018_04_01-08.csv", "url.csv", 1024, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), "any", None)))))
      ))

      modelTest.statementsForAllEoris.head.groups must be(statementPeriodsByMonth)
    }

    "have no files" in {
      val currentEoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), Seq.empty, Seq.empty)
      val model = vmDutyDefermentAccount(someDan, Seq(currentEoriStatements), someLinkId)
      model.hasCurrentStatements must be(false)
    }

    "set isRequestedAvailable to false" when {
      "there are no requested statements available" in {
        val eoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), someStatementFiles, Seq.empty)
        val model = vmDutyDefermentAccount(someDan, Seq(eoriStatements), someLinkId)
        model.hasRequestedStatements must be(false)
      }
    }

    "set hasRequestedStatements to true" when {
      "there are requested statements available" in {
        val eoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), Seq.empty, someStatementFiles)
        val model = vmDutyDefermentAccount(someDan, Seq(eoriStatements), someLinkId)
        model.hasRequestedStatements must be(true)
      }
    }

    "have files for the current eori" in {
      val currentEoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), someStatementFiles, Seq.empty)
      val model = vmDutyDefermentAccount(someDan, Seq(currentEoriStatements), someLinkId)
      model.hasCurrentStatements must be(true)
    }

    "have files for a historic eori" in {
      val currentEoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), Nil, Nil)
      val historicStatements = Seq(DutyDefermentStatementsForEori(EoriHistory(someHistoricEori, None, None), someStatementFiles, Seq.empty))
      val model = vmDutyDefermentAccount(someDan, currentEoriStatements +: historicStatements, someLinkId)
      model.hasCurrentStatements must be(true)
    }

    "be in the same month" in {
      val currentEoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), someStatementFiles, Seq.empty)
      val historicEoriFiles = Seq(DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None)))
      val historicStatements = Seq(DutyDefermentStatementsForEori(EoriHistory(someHistoricEori, None, None), historicEoriFiles, Seq.empty))
      val model = vmDutyDefermentAccount(someDan, currentEoriStatements +: historicStatements, someLinkId)
      model.isSameMonth("CurrentEori", someHistoricEori) must be(true)
    }

    "be in different months" in {
      val currentEoriFiles = Seq(DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 4, 25, 2018, 4, 30, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None)))
      val currentEoriStatements = DutyDefermentStatementsForEori(EoriHistory("CurrentEori", None, None), currentEoriFiles, Seq.empty)
      val historicEoriFiles = Seq(DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", someFileSize, DutyDefermentStatementFileMetadata(2018, 5, 1, 2018, 5, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None)))
      val historicStatements = Seq(DutyDefermentStatementsForEori(EoriHistory(someHistoricEori, None, None), historicEoriFiles, Seq.empty))
      val model = vmDutyDefermentAccount(someDan, currentEoriStatements +: historicStatements, someLinkId)
      model.isSameMonth("CurrentEori", someHistoricEori) must be(false)
    }
  }
}

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

import uk.gov.hmrc.customs.financials.controllers.OrderedByEoriHistory
import uk.gov.hmrc.customs.financials.domain.{DutyDefermentStatementFile, EoriHistory}

case class DutyDefermentStatementsForEori(eoriHistory: EoriHistory, currentStatements: Seq[DutyDefermentStatementFile], requestedStatements: Seq[DutyDefermentStatementFile])
  extends OrderedByEoriHistory[DutyDefermentStatementsForEori] {

  import uk.gov.hmrc.customs.financials.services.DateConverters.OrderedLocalDate

  private val currentStatementsByPeriod: Seq[DutyDefermentStatementPeriod] = groupByPeriod(currentStatements)

  val groups: Seq[DutyDefermentStatementPeriodsByMonth] = groupByMonthAndYear(currentStatementsByPeriod)

  private val requestedStatementsByPeriod: Seq[DutyDefermentStatementPeriod] = groupByPeriod(requestedStatements)

  val groupsRequested: Seq[DutyDefermentStatementPeriodsByMonth] = groupByMonthAndYear(requestedStatementsByPeriod)

  private def groupByPeriod(files: Seq[DutyDefermentStatementFile]): Seq[DutyDefermentStatementPeriod] = {
    files.groupBy(file => (file.metadata.fileRole, file.startDate, file.endDate, file.metadata.defermentStatementType)).map { case (_, periodFiles) =>
      DutyDefermentStatementPeriod(
        periodFiles.head.metadata.fileRole,
        periodFiles.head.metadata.defermentStatementType,
        periodFiles.head.monthAndYear,
        periodFiles.head.startDate,
        periodFiles.head.endDate,
        periodFiles.sorted)
    }.toSeq.sorted
  }

  private def groupByMonthAndYear(statementPeriods: Seq[DutyDefermentStatementPeriod]): Seq[DutyDefermentStatementPeriodsByMonth] = {
    val monthYearSorted = statementPeriods.groupBy(_.monthAndYear).toSeq.sortWith(_._1 > _._1)
    monthYearSorted.map {
      case (monthAndYear, statementPeriods) => DutyDefermentStatementPeriodsByMonth(monthAndYear, statementPeriods
        .sortWith(_.startDate > _.startDate)
        .sortWith(_.defermentStatementType < _.defermentStatementType))
    }
  }

}

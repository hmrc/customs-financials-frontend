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

import play.api.i18n.Messages
import uk.gov.hmrc.customs.financials.domain.DDStatementType.{Excise, Supplementary}
import uk.gov.hmrc.customs.financials.domain.DutyDefermentStatementFile
import uk.gov.hmrc.customs.financials.views.helpers.Formatters

object SdesFileViewModels {

  implicit class DutyDefermentStatementFileViewModel(file: DutyDefermentStatementFile) {

    def downloadLinkAriaLabel()(implicit messages: Messages): String = {
      lazy val endDateMonthAndYear = Formatters.dateAsMonthAndYear(file.endDate)
      lazy val endDateDayMonthAndYear = Formatters.dateAsDayMonthAndYear(file.endDate)
      lazy val startDateDay = Formatters.dateAsDay(file.startDate)
      lazy val fileSize = Formatters.fileSize(file.size)

      file.metadata.defermentStatementType match {
        case Supplementary => messages("cf.account.detail.supplementary-download-link", file.fileFormat, endDateMonthAndYear, fileSize)
        case Excise => messages("cf.account.detail.excise-download-link", file.fileFormat, endDateMonthAndYear, fileSize)
        case _ => messages("cf.account.detail.download-link", file.fileFormat, startDateDay, endDateDayMonthAndYear, fileSize)
      }
    }

  }

}

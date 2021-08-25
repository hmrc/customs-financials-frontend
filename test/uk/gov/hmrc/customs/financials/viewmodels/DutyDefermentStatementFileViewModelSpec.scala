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

import play.api.i18n._
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.domain.DDStatementType._
import uk.gov.hmrc.customs.financials.domain.FileFormat.Pdf
import uk.gov.hmrc.customs.financials.domain.FileRole.DutyDefermentStatement
import uk.gov.hmrc.customs.financials.domain.{DDStatementType, DutyDefermentStatementFile, DutyDefermentStatementFileMetadata}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.viewmodels.SdesFileViewModels.DutyDefermentStatementFileViewModel

class DutyDefermentStatementFileViewModelSpec() extends SpecBase {

  def ddStatementFile(statementType: DDStatementType) =
    DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", 1024,
      DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, statementType, Some(true), Some("BACS"), "any", None))

  "DutyDefermentStatementFileViewModel" should {
    "have download link voice over available" in new Setup {
      running(app) {
        ddStatementFile(Weekly).downloadLinkAriaLabel() must be("cf.account.detail.download-link")
      }

    }

    "have download link voice over available for supplementary statements" in new Setup {
      running(app) {
        ddStatementFile(Supplementary).downloadLinkAriaLabel() must be("cf.account.detail.supplementary-download-link")
      }
    }

    "have download link voice over available for excise summary statements" in new Setup {
      running(app) {
        ddStatementFile(Excise).downloadLinkAriaLabel() must be("cf.account.detail.excise-download-link")
      }
    }
  }

  trait Setup {
    val app = application().build()
    implicit val messages: Messages = Helpers.stubMessages()
  }
}

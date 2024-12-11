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

import domain.DutyPaymentMethod.{CDS, CHIEF}
import domain.FileFormat.Pdf
import domain.FileRole.{PostponedVATAmendedStatement, PostponedVATStatement}

import play.api.Application
import play.api.i18n.Messages
import utils.SpecBase
import utils.MustMatchers

import java.time.LocalDate

class PostponedVatStatementGroupSpec extends SpecBase with MustMatchers {

  "collectFiles" should {
    "return correct output when amended is true, file role is PostponedVATAmendedStatement " +
      "and source is CDS" in new Setup {
        pVatStatGroupForCdsAndFileRolePostVatAmendStat.collectFiles(amended = true, CDS) mustBe
          pVatFilesForSourceCdsAndFileRolePostVatAmendStat
      }

    "return correct output when amended is true, file role is PostponedVATAmendedStatement " +
      "and source is CHIEF" in new Setup {
        pVatStatGroupForChiefAndFileRolePostVatAmendStat.collectFiles(amended = true, CHIEF) mustBe
          pVatFilesForSourceChiefAndFileRolePostVatAmendStat
      }

    "return correct output when amended is false, file role is PostponedVATStatement and source is CDS" in new Setup {
      pVatStatGroupForCdsAndFileRolePostVatStat.collectFiles(amended = false, CDS) mustBe
        pVatFilesForSourceCdsAndFileRolePostVatStat
    }

    "return correct output when amended is false, file role is PostponedVATStatement and source is Chief" in new Setup {
      pVatStatGroupForChiefAndFileRolePostVatStat.collectFiles(amended = false, CHIEF) mustBe
        pVatFilesForSourceChiefAndFileRolePostVatStat
    }
  }

  "noStatements" should {
    "return true when there is no matching file for the given source" in new Setup {
      pVatStatGroupForCdsAndFileRolePostVatAmendStat.noStatements mustBe true
    }

    "return false when there are matching file for the given source" in new Setup {
      pVatStatGroupForChiefAndFileRolePostVatStat.noStatements mustBe false
    }
  }

  "periodId" should {
    "return correct output" in new Setup {
      pVatStatGroupForCdsAndFileRolePostVatAmendStat.periodId mustBe "period-october-2023"
    }
  }

  "compare" should {
    "sort the order correctly" in new Setup {
      val year2021 = 2021

      val pVatStatGroupYear2023: PostponedVatStatementGroup = pVatStatGroupForChiefAndFileRolePostVatStat
      val pVatStatGroupYear2021: PostponedVatStatementGroup =
        pVatStatGroupForChiefAndFileRolePostVatStat.copy(startDate = LocalDate.of(year2021, month, day))

      List(pVatStatGroupYear2023, pVatStatGroupYear2021).sorted mustBe
        List(pVatStatGroupYear2021, pVatStatGroupYear2023)
    }
  }

  trait Setup {
    val eori = "test_eori"

    val year  = 2023
    val month = 10
    val day   = 8

    val startYear        = 2021
    val periodStartMonth = 10
    val source           = "test_source"
    val fileName         = "test_file"
    val downloadUrl      = "test_url.com"
    val size             = 2048L

    val app: Application = application().build()

    implicit val msgs: Messages = messages(app)
    val date: LocalDate         = LocalDate.of(year, month, day)

    val metaDataWithSourceCdsAndFileRolePostVatAmendStat: PostponedVatStatementFileMetadata =
      PostponedVatStatementFileMetadata(startYear, periodStartMonth, Pdf, PostponedVATAmendedStatement, CDS, None)

    val metaDataWithSourceChiefAndFileRolePostVatAmendStat: PostponedVatStatementFileMetadata =
      PostponedVatStatementFileMetadata(startYear, periodStartMonth, Pdf, PostponedVATAmendedStatement, CHIEF, None)

    val metaDataWithSourceCdsAndFileRolePostVatStat: PostponedVatStatementFileMetadata =
      PostponedVatStatementFileMetadata(startYear, periodStartMonth, Pdf, PostponedVATStatement, CDS, None)

    val metaDataWithSourceChiefAndFileRolePostVatStat: PostponedVatStatementFileMetadata =
      PostponedVatStatementFileMetadata(startYear, periodStartMonth, Pdf, PostponedVATStatement, CHIEF, None)

    val pVatFilesForSourceCdsAndFileRolePostVatAmendStat: Seq[PostponedVatStatementFile] = Seq(
      PostponedVatStatementFile(fileName, downloadUrl, size, metaDataWithSourceCdsAndFileRolePostVatAmendStat, eori)
    )

    val pVatFilesForSourceChiefAndFileRolePostVatAmendStat: Seq[PostponedVatStatementFile] = Seq(
      PostponedVatStatementFile(fileName, downloadUrl, size, metaDataWithSourceChiefAndFileRolePostVatAmendStat, eori)
    )

    val pVatFilesForSourceCdsAndFileRolePostVatStat: Seq[PostponedVatStatementFile] = Seq(
      PostponedVatStatementFile(fileName, downloadUrl, size, metaDataWithSourceCdsAndFileRolePostVatStat, eori)
    )

    val pVatFilesForSourceChiefAndFileRolePostVatStat: Seq[PostponedVatStatementFile] = Seq(
      PostponedVatStatementFile(fileName, downloadUrl, size, metaDataWithSourceChiefAndFileRolePostVatStat, eori)
    )

    val pVatStatGroupForCdsAndFileRolePostVatAmendStat: PostponedVatStatementGroup =
      PostponedVatStatementGroup(date, pVatFilesForSourceCdsAndFileRolePostVatAmendStat)

    val pVatStatGroupForChiefAndFileRolePostVatAmendStat: PostponedVatStatementGroup =
      PostponedVatStatementGroup(date, pVatFilesForSourceChiefAndFileRolePostVatAmendStat)

    val pVatStatGroupForCdsAndFileRolePostVatStat: PostponedVatStatementGroup =
      PostponedVatStatementGroup(date, pVatFilesForSourceCdsAndFileRolePostVatStat)

    val pVatStatGroupForChiefAndFileRolePostVatStat: PostponedVatStatementGroup =
      PostponedVatStatementGroup(date, pVatFilesForSourceChiefAndFileRolePostVatStat)
  }
}

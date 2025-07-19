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

import domain.FileFormat.Csv
import domain.FileRole.StandingAuthority
import utils.TestData.{DAY_1, MONTH_6, TEST_EORI, YEAR_2022}
import utils.{MustMatchers, SpecBase}
import play.api.libs.json.Json

class AuditModelSpec extends SpecBase with MustMatchers {

  "DownloadStatementAuditData.apply" should {

    "populate the object with correct contents" in {
      val sdesMetaData                 = StandingAuthorityMetadata(YEAR_2022, MONTH_6, DAY_1, Csv, StandingAuthority)
      val downloadStatementAuditDataOb = DownloadStatementAuditData(sdesMetaData, TEST_EORI)

      downloadStatementAuditDataOb.auditData mustBe Map(
        "eori"             -> "GB12345678",
        "fileRole"         -> "StandingAuthority",
        "periodStartDay"   -> "1",
        "fileFormat"       -> "CSV",
        "periodStartMonth" -> "6",
        "periodStartYear"  -> "2022"
      )
    }
  }

  "DownloadStatementAuditData.downloadStatementAuditDataWrites" should {

    "generate the correct output using Json Writes" in {
      val sdesMetaData                 = StandingAuthorityMetadata(YEAR_2022, MONTH_6, DAY_1, Csv, StandingAuthority)
      val downloadStatementAuditDataOb = DownloadStatementAuditData(sdesMetaData, TEST_EORI)

      val downloadStatAuditDataJsString =
        """{"auditData":{
          |"eori":"GB12345678",
          |"fileRole":"StandingAuthority",
          |"periodStartDay":"1",
          |"fileFormat":"CSV",
          |"periodStartMonth":"6",
          |"periodStartYear":"2022"}
          |}""".stripMargin

      Json.toJson(downloadStatementAuditDataOb) mustBe Json.parse(downloadStatAuditDataJsString)
    }
  }
}

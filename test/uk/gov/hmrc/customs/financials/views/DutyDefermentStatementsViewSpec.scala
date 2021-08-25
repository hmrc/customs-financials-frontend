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

package uk.gov.hmrc.customs.financials.views

import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.DDStatementType.{Excise, Supplementary, Weekly}
import uk.gov.hmrc.customs.financials.domain.FileFormat.{Csv, Pdf}
import uk.gov.hmrc.customs.financials.domain.FileRole.DutyDefermentStatement
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.viewmodels.{DutyDefermentAccount, DutyDefermentStatementsForEori}
import uk.gov.hmrc.customs.financials.views.html.duty_deferment_account

import java.time.LocalDate

class DutyDefermentStatementsViewSpec extends SpecBase {

  "Duty Deferment Statements view" should {
    "have a header section" which {
      "displays the page heading" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("statements-heading").text mustBe "Duty deferment statements"
        }
      }

      "displays the account heading" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("eori-heading").getElementsByClass("govuk-caption-xl").text mustBe "Account: account-number-1"
        }
      }

      "displays a link to requested historic statements when there are some available" in new Setup {
        running(app) {
          dutyDefermentAccountRequestedStatementsView.getElementById("request-statement-link").text mustBe "requested statements"
        }
      }

      "does not display a link to requested historic statements when there are none available" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementsContainingText("requested statements").isEmpty mustBe true
        }
      }

      "displays link to historic statements request when the feature is enabled" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("historic-statement-request").text mustBe "We only show statements for the last 6 months. If required, you can request older statements."
        }
      }
    }

    "have a statement list section" which {
      "displays a duty deferment statements by period, in descending date order" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("statements-month-heading-0-2018-4").text mustBe "April 2018"
          dutyDefermentAccountHistoricEoriView.getElementById("statements-list-0-2018-4-row-3-date-cell").text mustBe "1 to 8 April"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-3-download-link").text mustBe "PDF (1KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-3-download-link").attr("aria-label") mustBe "Download 1 to 8 April 2018 PDF (1KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-3-download-link").text mustBe "CSV (1KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-3-download-link").attr("aria-label", "Download 1 to 8 April 2018 CSV (1KB)").text mustBe "CSV (1KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("statements-list-0-2018-4-row-2-date-cell").text mustBe "9 to 15 April"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-2-download-link").text mustBe "PDF (2KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-2-download-link").text mustBe "CSV (2KB)"
        }
      }

      "displays a message when a CSV or PDF duty deferment statement is missing" in new Setup {
        running(app) {
          dutyDefermentAccountMissingCsvView.getElementById("csv-0-2018-4-row-0-unavailable").text mustBe "Unavailable"
          dutyDefermentAccountMissingCsvView.getElementById("csv-0-2018-4-row-0-hidden").text mustBe "CSV for 1 to 8 April 2018 unavailable"
        }
      }

      "displays a supplementary end of month duty deferment statement" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("statements-list-0-2018-4-row-1-date-cell").text mustBe "Supplementary end of month"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-1-download-link").text mustBe "PDF (2KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-1-download-link").attr("aria-label") mustBe "Download supplementary end of month for April 2018 PDF (2KB)"
        }
      }

      "displays unavailable for a missing supplementary end of month CSV" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-1-unavailable").text mustBe "Unavailable"
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-1-hidden").text mustBe "Supplementary end of month CSV for April 2018 unavailable"
        }
      }

      "displays a excise summary duty deferment statement" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("statements-list-0-2018-4-row-0-date-cell").text mustBe "Excise summary"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-0-download-link").text mustBe "PDF (2KB)"
          dutyDefermentAccountHistoricEoriView.getElementById("pdf-0-2018-4-row-0-download-link").attr("aria-label") mustBe "Download excise summary for April 2018 PDF (2KB)"
        }
      }

      "displays unavailable for a missing excise summary CSV" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-0-unavailable").text mustBe "Unavailable"
          dutyDefermentAccountHistoricEoriView.getElementById("csv-0-2018-4-row-0-hidden").text mustBe "Excise summary CSV for April 2018 unavailable"
        }
      }
    }

    "have a footer section" which {

      "displays the can`t find what you`re looking for message" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("missing-documents-guidance-heading").text mustBe "Can’t find the statement you’re looking for?"
          dutyDefermentAccountHistoricEoriView.getElementById("missing-documents-guidance-text1").text mustBe "Statements for import declarations made in CHIEF are not available in this service."
          dutyDefermentAccountHistoricEoriView.getElementById("missing-documents-guidance-text2").text mustBe "Statements are only generated for periods in which you imported goods."
        }
      }

      "display a help and support message" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("help_and_support").text mustBe "Help and support If you are having issues, phone 0300 059 4243. Open 9am to 12pm, and 2pm to 4pm Monday to Friday (closed bank holidays)."
        }
      }

      "displays the direct debit payments message" in new Setup {
        running(app) {
          dutyDefermentAccountHistoricEoriView.getElementById("direct-debit-info").text mustBe "HMRC will charge your bank each month for Duty and VAT on or after the 15th, and Excise on or after the 29th."
        }
      }
    }

    "display statements for historic EORIs" in new Setup {
      running(app) {
        dutyDefermentAccountHistoricEoriView.getElementById("historic-eori-1").text mustBe "EORI: historic-two"
        dutyDefermentAccountHistoricEoriView.getElementById("statements-list-1-2018-4-row-0-date-cell").text mustBe "Excise summary"
      }
    }

    "not display heading and section for historic EORIs that do not have statements" in new Setup {
      running(app) {
        dutyDefermentAccountHistoricEoriNoStatementView.getElementsContainingText("EORI: historic-two").isEmpty mustBe true
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")

    val someDan = "1234567"
    val dutyDefermentStatementFiles = List(
      DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, Some("id"))),
      DutyDefermentStatementFile("2018_04_01-08.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, Some("id"))),
      DutyDefermentStatementFile("2018_04_09-15.pdf", "url2.pdf", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, Some("id"))),
      DutyDefermentStatementFile("2018_04_09-15.csv", "url2.csv", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, Some("id")))
    )

    val dutyDefermentStatementFilesWithSupplementary = dutyDefermentStatementFiles ++ List(
      DutyDefermentStatementFile("2018_04_01-30.pdf", "url3.pdf", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 30, Pdf, DutyDefermentStatement, Supplementary, Some(true), Some("BACS"), someDan, Some("id")))
    )

    val dutyDefermentStatementFilesWithExcise = dutyDefermentStatementFiles ++ List(
      DutyDefermentStatementFile("2018_04_01-30.pdf", "url3.pdf", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 30, Pdf, DutyDefermentStatement, Excise, Some(true), Some("BACS"), someDan, Some("id")))
    )

    val dutyDefermentStatementFilesWithExciseWithSupplementary = dutyDefermentStatementFiles ++ dutyDefermentStatementFilesWithExcise ++ dutyDefermentStatementFilesWithSupplementary


    val dutyDefermentStatementFilesWithMissingCsv = List(
      DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, Some("id")))
    )

    val today = LocalDate.now()
    val oneYearAgo = today.minusYears(1)
    val twoYearsAgo = today.minusYears(2)
    val threeYearsAgo = today.minusYears(3)

    val historicOne = EoriHistory("historic-one", Some(oneYearAgo), None)
    val historicTwo = EoriHistory("historic-two", Some(twoYearsAgo), Some(oneYearAgo.minusMonths(1)))
    val historicThree = EoriHistory("historic-three", Some(threeYearsAgo), Some(twoYearsAgo.minusMonths(1)))

    val statementsForNonEmptyHistoricOne = DutyDefermentStatementsForEori(
      historicOne,
      dutyDefermentStatementFilesWithExciseWithSupplementary,
      Seq.empty
    )

    val statementsForNonEmptyHistoricOneNoCsv = DutyDefermentStatementsForEori(
      historicOne,
      dutyDefermentStatementFilesWithMissingCsv,
      Seq.empty
    )

    val requestedStatementsForNonEmptyHistoricOne = DutyDefermentStatementsForEori(
      historicOne,
      Seq.empty,
      dutyDefermentStatementFiles
    )

    val statementsForNonEmptyHistoricTwo = statementsForNonEmptyHistoricOne.copy(eoriHistory = historicTwo)
    val statementsForEmptyHistoricTwo = DutyDefermentStatementsForEori(historicTwo, Seq.empty, Seq.empty)
    val statementsForEmptyHistoricThree = statementsForEmptyHistoricTwo.copy(eoriHistory = historicThree)

    val viewModelWithHistoricEori = DutyDefermentAccount(
      "account-number-1",
      Seq(statementsForNonEmptyHistoricOne, statementsForNonEmptyHistoricTwo, statementsForEmptyHistoricThree),
      "link-id-1")

    val viewModelWithRequestedStatements = DutyDefermentAccount(
      "account-number-1",
      Seq(requestedStatementsForNonEmptyHistoricOne),
      "link-id-1")

    val viewModelWithMissingCSV = DutyDefermentAccount(
      "account-number-1", Seq(statementsForNonEmptyHistoricOneNoCsv), "link-id-1")

    val viewModelHistoricEoriNoStatements = DutyDefermentAccount(
      "account-number-1",
      Seq(statementsForEmptyHistoricTwo, statementsForEmptyHistoricThree),
      "link-id-2")

    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]



    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    val dutyDefermentAccountHistoricEoriView = Jsoup.parse(app.injector.instanceOf[duty_deferment_account].apply(viewModelWithHistoricEori).body)
    val dutyDefermentAccountRequestedStatementsView = Jsoup.parse(app.injector.instanceOf[duty_deferment_account].apply(viewModelWithRequestedStatements).body)
    val dutyDefermentAccountMissingCsvView = Jsoup.parse(app.injector.instanceOf[duty_deferment_account].apply(viewModelWithMissingCSV).body)
    val dutyDefermentAccountHistoricEoriNoStatementView = Jsoup.parse(app.injector.instanceOf[duty_deferment_account].apply(viewModelHistoricEoriNoStatements).body)
  }
}

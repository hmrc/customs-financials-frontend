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

package views



import config.AppConfig
import domain.FileFormat.Pdf
import domain.FileRole.SecurityStatement
import domain.{EoriHistory, SecurityStatementFile, SecurityStatementFileMetadata, SecurityStatementsByPeriod, SecurityStatementsForEori}
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.SpecBase
import viewmodels.SecurityStatementsViewModel
import views.html.securities.security_statements

import java.time.LocalDate

class SecuritiesStatementsViewSpec extends SpecBase  {

  implicit val request = FakeRequest("GET", "/some/resource/path")

  val files = List(SecurityStatementFile("statementfile_00", "download_url_00", 99L, SecurityStatementFileMetadata(2017, 12, 28, 2018, 1, 1, Pdf, SecurityStatement, "someEori", 1024L, "0000000", None)))
  val files2 = List(SecurityStatementFile("statementfile_01", "download_url_01", 99L, SecurityStatementFileMetadata(2018, 1, 2, 2018, 1, 31, Pdf, SecurityStatement, "someEori", 2048L, "1123456", None)))
  val files3 = List(SecurityStatementFile("statementfile_00", "download_url_00", 99L, SecurityStatementFileMetadata(2019, 1, 1, 2019, 12, 31, Pdf, SecurityStatement, "GB67890", 4096L, "0000000", None)))

  val currentStatements = Seq(
    SecurityStatementsByPeriod(LocalDate.of(2017, 1, 1), LocalDate.of(2017, 7, 31), files),
    SecurityStatementsByPeriod(LocalDate.of(2018, 8, 1), LocalDate.of(2018, 11, 30), files2),
    SecurityStatementsByPeriod(LocalDate.of(2018, 1, 12), LocalDate.of(2018, 12, 31), Seq.empty))

  val requestedStatements = Seq(
    SecurityStatementsByPeriod(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 11, 30), files3))

  val statementsForEori1 = SecurityStatementsForEori(EoriHistory("GB12345", None, None), currentStatements, Seq.empty)
  val requestedStatementsForEori2 = SecurityStatementsForEori(EoriHistory("GB67890", None, None), requestedStatements, Seq.empty)
  val noStatementsForEori3 = SecurityStatementsForEori(EoriHistory("GB888666", None, None), Seq.empty, Seq.empty)

  val model = SecurityStatementsViewModel(Seq(statementsForEori1, requestedStatementsForEori2, noStatementsForEori3))

  "Securities Statements view" should {
      "contain a historic eori number heading with statements list" when {
        "there are current statements available" in new Setup{
          running(app) {
            val historicEoriStatements = SecurityStatementsForEori(EoriHistory("GB123456", None, None), currentStatements, Seq.empty)
            val viewModel = SecurityStatementsViewModel(Seq(statementsForEori1, historicEoriStatements))
            val contentWithHistoricEori = Jsoup.parse(view(viewModel)(implicitly, implicitly, appConfig).body)
            contentWithHistoricEori.containsElementById("historic-eori-1")
            contentWithHistoricEori.containsElementById("statements-list-1")
          }
        }
      }

      "not contain a historic eori number heading with statements list" when {
        "there are no current statements available" in new Setup{
          running(app) {
            content.notContainElementById("historic-eori-2")
            content.notContainElementById("statements-list-2")
          }
        }
      }

      "contain a statements list for current eori" in new Setup{
        running(app) {
          content.notContainElementById("historic-eori-0")
          content.containsElementById("statements-list-0")
          content.getElementById("statements-list-0-row-0-date-cell").text() shouldBe "1 January 2017 to 31 July 2017"
          content.getElementById("statements-list-0-row-0-link-cell").text() shouldBe "PDF (1KB)"
          content.getElementById("statements-list-0-row-1-date-cell").text() shouldBe "1 August 2018 to 30 November 2018"
          content.getElementById("statements-list-0-row-1-link-cell").text() shouldBe "PDF (2KB)"
        }
      }

      "display statements for historic eori" in new Setup{
        running(app) {
          content.containsElementById("historic-eori-1")
          content.containsElementById("statements-list-1")
          content.getElementById("statements-list-1-row-0-date-cell").text() shouldBe "1 February 2019 to 30 November 2019"
          content.getElementById("statements-list-1-row-0-link-cell").text() shouldBe "PDF (4KB)"
        }
      }

      "display unavailable section when there is no statement file" in new Setup{
        running(app) {
          content.containsElementById("statements-list-0-row-2-unavailable")
        }
      }

      "contain a missing documents section" in new Setup{
        running(app) {
          content.containsElementById("missing-documents-guidance")
        }
      }

      "contain a historic statement request section" in new Setup{
        running(app) {
          content.containsElementById("historic-statement-request")
        }
      }

      "contain a help and support section" in new Setup{
        running(app) {
          content.containsElementById("help_and_support")
        }
      }
  }

  trait Setup extends I18nSupport {
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val view = app.injector.instanceOf[security_statements]
    val content = Jsoup.parse(view(model).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

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

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import play.api
import play.api.inject
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.actionbuilders.{FakePvatIdentifierAction, FakePvatWithHistoricIdentifierAction, PvatIdentifierAction}
import uk.gov.hmrc.customs.financials.domain.DutyPaymentMethod._
import uk.gov.hmrc.customs.financials.domain.FileFormat.{Csv, Pdf}
import uk.gov.hmrc.customs.financials.domain.FileRole.{PostponedVATAmendedStatement, PostponedVATStatement}
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.services.{ApiService, DataStoreService, DocumentService}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.JavaConverters._
import scala.concurrent.Future

class PostponedVatControllerSpec extends SpecBase {

  "The Postponed VAT statements page" should {
    "return OK" in new Setup {

      running(app) {
        val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "inform API to remove Postponed VAT Statement notifications" in new Setup {

      running(app) {
        val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
        val result = route(app, request).value
        status(result) mustBe OK
        verify(mockApiService).deleteNotification(eqTo(newUser().eori), eqTo(PostponedVATStatement))(any)
      }
    }


    "summary section" should {
      "display the EORI" in new Setup {

        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.getElementById("eori").text mustBe newUser().eori
        }
      }

      "have heading 'Your postponed import VAT statements'" in new Setup {

        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.getElementsByTag("h1").text mustBe s"Postponed import VAT statements"
        }
      }

      "display postponed VAT statements" in new Setup {
        val pvatStatementFilesWithMissingCsv = List(
          PostponedVatStatementFile(
            "pdf_file", "download_url_pdf",
            1024L,
            PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(1).getMonthValue, Pdf, PostponedVATStatement, CDS, None),
            ""
          )
        )

        when(mockDocumentService.getPostponedVatStatements(eqTo(newUser().eori))(ArgumentMatchers.any()))
          .thenReturn(Future.successful(pvatStatementFilesWithMissingCsv))

        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          val actualVatStatements = html.select("ul.govuk-list li a").asScala.map(e => e.text()).toList

          val expectedVatstatements = List(
            "CDS statement - PDF (1KB)"
          )

          actualVatStatements mustBe expectedVatstatements
        }
      }

      "display historic and current postponed VAT statements" in new Setup {
        val historicEori = "GB12345"
        val historicEoriPvatFiles = List(
          PostponedVatStatementFile("2027_11_pdf", "download_url_2021_11_pdf", 9999999999L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(1).getMonthValue, Pdf, PostponedVATStatement, CDS, None), ""))
        when(mockDocumentService.getPostponedVatStatements(eqTo(newUser(Seq(EoriHistory(historicEori, None, None))).eori))(any))
          .thenReturn(Future.successful(postponedVatStatementFiles))
        when(mockDocumentService.getPostponedVatStatements(eqTo(historicEori))(any))
          .thenReturn(Future.successful(historicEoriPvatFiles))
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        val appWithEoriHistory = application(Seq(EoriHistory(historicEori, None, None))).overrides(
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService),
          inject.bind[PvatIdentifierAction].to[FakePvatWithHistoricIdentifierAction]
        ).build()
        running(appWithEoriHistory) {

          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(appWithEoriHistory, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          val expectedPVatStatements = List(
            "CDS statement - PDF (4KB)",
            "CDS statement - PDF (10000.0MB)",
            "CDS amended statement - PDF (8KB)",
            "CDS statement - PDF (4KB)",
            "CDS statement - PDF (4KB)",
            "CDS statement - PDF (1KB)",
            "CDS statement - PDF (1.3MB)"
          )

          val actualPVatStatements = html.select("ul.govuk-list li a").asScala.map(e => e.text()).toList

          actualPVatStatements mustBe expectedPVatStatements
        }
      }



      "display amended postponed VAT statements before originals" in new Setup {

        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          val actualPVATStatements = html
            .select("ul.govuk-list li a")
            .asScala.filter(e => e.attr("href").contains("/some-url"))
            .map(_.text())
            .toList

          val expectedPVATStatements = List(
            "CDS statement - PDF (4KB)",
            "CDS amended statement - PDF (8KB)",
            "CDS statement - PDF (4KB)",
            "CDS statement - PDF (4KB)",
            "CDS statement - PDF (1KB)",
            "CDS statement - PDF (1.3MB)"
          )
          actualPVATStatements mustBe expectedPVATStatements
        }
      }

      "display Help and support heading and message" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.getElementsContainingText("Help and support").isEmpty mustBe false
          html.select("#help-message").hasText
        }
      }


      "shows files for all available months that contain a valid Pvat file format" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          val actualMonths = html
            .select("div[id*=period-] h2")
            .asScala.map(_.text)
            .toList

          actualMonths must be(List(
            s"${month(date.minusMonths(1))} ${date.getYear}",
            s"${month(date.minusMonths(2))} ${date.getYear}",
            s"${month(date.minusMonths(3))} ${date.getYear}",
            s"${month(date.minusMonths(4))} ${date.getYear}",
            s"${month(date.minusMonths(5))} ${date.getYear}",
            s"${month(date.minusMonths(6))} ${date.getYear}"
          ))

        }
      }

      "has statements period text" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          val hasgeneratedPeriodText = html.getAllElements.asScala.map(_.text()).find {
            _ == "Statements are only generated for periods in which you imported goods."
          }
          val hasOnlyShowText = html.getAllElements.asScala.map(_.text()).find {
            _ == "We only show statements for the last 6 months. If required, you can request older statements."
          }

          hasgeneratedPeriodText.isDefined mustBe true
          hasOnlyShowText.isDefined mustBe true
        }
      }

      "show unauthorised page" when {
        "session id is missing from the request" in {
          val app = application().overrides(api.inject.bind[PvatIdentifierAction].to[FakePvatIdentifierAction]).build()
          running(app) {
            val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url)
            val result = route(app, request).value
            status(result) mustBe UNAUTHORIZED
          }
        }
      }

      "not call SDES when PostponedVatHolding feature switch is enabled " in {

        val mockDocumentService = mock[DocumentService]
        val mockDataStoreService = mock[DataStoreService]

        val app = application().overrides(
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService)
        ).build()



        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          await(route(app, request).value)
          verify(mockDataStoreService, never)
            .getAllEoriHistory(ArgumentMatchers.anyString)(any[HeaderCarrier])
          verify(mockDocumentService, never)
            .getPostponedVatStatements(any[String])(any[HeaderCarrier])
        }
      }

      "display message when no PVAT statements available" in new Setup {
        when(mockDocumentService.getPostponedVatStatements(eqTo(newUser().eori))(any))
          .thenReturn(Future.successful(List.empty))

        running(app) {
          val request = fakeRequest(GET, routes.PostponedVatController.show(location = Some("CDS")).url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          contentAsString(result) must include regex "There were no statements in June"
        }
      }
    }
  }

  trait Setup {


    val date: LocalDate = LocalDate.now()
    def month(date: LocalDate): String = DateTimeFormatter.ofPattern("MMMM").format(date)

    val postponedVatStatementFiles = List(
      PostponedVatStatementFile("name_04", "/some-url", 111L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(7).getMonthValue, Csv, PostponedVATStatement, CDS, None), ""),
      PostponedVatStatementFile("name_04", "/some-url", 111L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(7).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_03", "/some-url", 111L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(4).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_02", "/some-url", 111L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(5).getMonthValue, Csv, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_01", "/some-url", 1300000L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(5).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_04", "/some-url", 8192L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(2).getMonthValue, Pdf, PostponedVATAmendedStatement, CDS,None), ""),
      PostponedVatStatementFile("name_02", "/some-url", 8192L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(2).getMonthValue, Csv, PostponedVATAmendedStatement, CDS,None), ""),
      PostponedVatStatementFile("name_04", "/some-url", 4096L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(3).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_03", "/some-url", 4096L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(2).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_03", "/some-url", 4096L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(1).getMonthValue, Csv, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_03", "/some-url", 4096L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(1).getMonthValue, Pdf, PostponedVATStatement, CDS,None), ""),
      PostponedVatStatementFile("name_02", "/some-url", 4096L, PostponedVatStatementFileMetadata(date.getYear, date.minusMonths(2).getMonthValue, Csv, PostponedVATStatement, CDS,None), "")

    )

    val mockDocumentService = mock[DocumentService]
    val mockApiService: ApiService = mock[ApiService]
    val mockDataStoreService = mock[DataStoreService]


    val app = application().overrides(
      inject.bind[DocumentService].toInstance(mockDocumentService),
      inject.bind[ApiService].toInstance(mockApiService),
      inject.bind[DataStoreService].toInstance(mockDataStoreService),
      inject.bind[PvatIdentifierAction].to[FakePvatIdentifierAction]
    ).build()

    when(mockDocumentService.getPostponedVatStatements(eqTo(newUser().eori))(any))
      .thenReturn(Future.successful(postponedVatStatementFiles))
    when(mockApiService.deleteNotification(any, any)(any))
      .thenReturn(Future.successful(true))
  }
}

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

import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import play.api.inject.bind
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.domain.DDStatementType.{Excise, Supplementary, Weekly}
import uk.gov.hmrc.customs.financials.domain.FileFormat.{Csv, Pdf}
import uk.gov.hmrc.customs.financials.domain.FileRole.DutyDefermentStatement
import uk.gov.hmrc.customs.financials.domain.{DefermentAccountAvailable, DutyDefermentStatementFile, _}
import uk.gov.hmrc.customs.financials.services.{ApiService, DataStoreService, DocumentService}
import uk.gov.hmrc.customs.financials.utils.SpecBase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.JavaConverters._
import scala.concurrent.Future

class DutyDefermentAccountControllerSpec extends SpecBase {

  "show account details" should {
    "display the current statements page with session cache disabled" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now()))))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "redirect User to accounts page to regenerate link if no link found in session cache" in new Setup {
      val invalidLinkId = "invalid_link_id"

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(invalidLinkId))(any))
        .thenReturn(Future.successful(None))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(invalidLinkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.CustomsFinancialsHomeController.index().url
      }
    }

    "have a statement list section" in new Setup {
      val someHistoricEori = "testEori2"

      val historicEoris = Seq(
        EoriHistory(newUser().eori, toOptionalDate("2017-03-20"), None),
        EoriHistory(someHistoricEori, toOptionalDate("1987-03-20"), toOptionalDate("2017-03-20"))
      )

      val fileForHistoricEori1: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 1, 2018, 3, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val fileForHistoricEori2: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 1, 2018, 3, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val sdesFileWithMetaDataCacheIdForHistoricEoriList = Seq(fileForHistoricEori1, fileForHistoricEori2)

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockDocumentService.getDutyDefermentStatements(eqTo(newUser().eori), eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdList))
      when(mockDocumentService.getDutyDefermentStatements(eqTo(someHistoricEori), eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdForHistoricEoriList))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByTag("h2").get(1).text mustBe "EORI: testEori2"

        val currentEoriStatements = html.select("dt").asScala.filter(_.id().contains("statements-list-0")).map(_.text()).toList

        currentEoriStatements mustBe List(
          "9 to 15 April",
          "1 to 8 April"
        )

        val historicStatements = html.select("dt").asScala.filter(_.id().contains("statements-list-1")).map(_.text()).toList

        historicStatements mustBe List("1 to 8 March")

        html.getElementsByTag("h2").first().text mustBe "April 2018"
        html.getElementsByTag("h2").get(2).text mustBe "March 2018"
      }
    }

    "displays a duty deferment statements for current and historic Eori, split within a month" in new Setup {

      val someHistoricEori = "testEori2"

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val dutyDefermentStatementFilesForCurrentEori1: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 9, 2018, 3, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val dutyDefermentStatementFilesForCurrentEori2: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 9, 2018, 3, 15, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val sdesFileWithMetaDataCacheIdForCurrentEoriList = Seq(dutyDefermentStatementFilesForCurrentEori1, dutyDefermentStatementFilesForCurrentEori2)

      when(mockDocumentService.getDutyDefermentStatements(ArgumentMatchers.eq(newUser().eori), eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdForCurrentEoriList))

      val dutyDefermentStatementFileForHistoricEori1: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 1, 2018, 3, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val dutyDefermentStatementFileForHistoricEori2: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_03_01-08.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 3, 1, 2018, 3, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val dutyDefermentStatementFileForHistoricEori3: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_02_20-27.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 2, 20, 2018, 2, 27, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
      val dutyDefermentStatementFileForHistoricEori4: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_02_20-27.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 2, 20, 2018, 2, 27, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))

      val sdesFileWithMetaDataCacheIdForHistoricEoriList =
        Seq(dutyDefermentStatementFileForHistoricEori1,
          dutyDefermentStatementFileForHistoricEori2,
          dutyDefermentStatementFileForHistoricEori3,
          dutyDefermentStatementFileForHistoricEori4)

      when(mockDocumentService.getDutyDefermentStatements(ArgumentMatchers.eq(someHistoricEori), eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdForHistoricEoriList))

      val historicEoris = List(EoriHistory(newUser().eori, toOptionalDate("2018-03-01"), None), EoriHistory(someHistoricEori, toOptionalDate("1987-03-20"), toOptionalDate("2018-02-01")))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()
      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsByTag("h2").get(1).text mustBe "EORI: testEori2"

        val statementTables = List(
          html.select("dt").asScala.filter(_.id().contains("statements-list-0")).map(_.text()).toList,
          html.select("dt").asScala.filter(_.id().contains("statements-list-1")).map(_.text()).toList,
          html.select("dt").asScala.filter(_.id().contains("statements-list-2")).map(_.text()).toList
        )

        statementTables.size mustBe 3
        statementTables.head mustBe List("9 to 15 March")

        statementTables(1).head mustBe "1 to 8 March"
        statementTables(1).tail mustBe List("20 to 27 February")

        val captions = html.select("h2").asScala.map(_.text())

        captions must contain("March 2018")
        captions must contain("February 2018")
      }
    }

    "displays a supplementary duty deferment statements after regular statements, with specific wording" in new Setup {

      when(mockDocumentService.getDutyDefermentStatements(eqTo(newUser().eori),
        eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdSupplementaryList))

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))
      val historicEoris = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        val actualDutyDefermentStatementGroups = html.select(".govuk-summary-list__row dt").asScala.map(_.text).toList
        val expectedDutyDefermentStatementGroups = List(
          "Supplementary end of month",
          "9 to 15 April",
          "1 to 8 April"
        )

        actualDutyDefermentStatementGroups mustBe expectedDutyDefermentStatementGroups
      }
    }

    "displays a no statements available message when no statements are available for DAN" in new Setup {
      when(mockDocumentService.getDutyDefermentStatements(eqTo(newUser().eori), eqTo(someDan))(any)).thenReturn(Future.successful(List()))
      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))
      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val historicEoris = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        html.getElementsContainingText(s"No statements available for deferment account $someDan").isEmpty mustBe false
      }
    }

    "displays an Excise duty deferment statements after regular statements, with specific wording" in new Setup {
      when(mockDocumentService.getDutyDefermentStatements(eqTo(newUser().eori),
        eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdExciseList))

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))
      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val historicEoris = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        val actualDutyDefermentStatementGroups = html.select("dt").asScala.map(_.text).toList
        val expectedDutyDefermentStatementGroups = List(
          "Excise summary",
          "9 to 15 April",
          "1 to 8 April"
        )

        actualDutyDefermentStatementGroups mustBe expectedDutyDefermentStatementGroups
      }
    }

    "displays duty deferment statements in order regular, supplementary then Excise, with specific wording" in new Setup {
      when(mockDocumentService.getDutyDefermentStatements(eqTo(newUser().eori),
        eqTo(someDan))(any)).thenReturn(Future.successful(sdesFileWithMetaDataCacheIdExciseAndSupplementaryList))

      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))
      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val historicEoris = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        val html = Jsoup.parse(contentAsString(result))

        val actualDutyDefermentStatementGroups = html.select("dt").asScala.map(_.text).toList
        val expectedDutyDefermentStatementGroups = List(
          "Excise summary",
          "Supplementary end of month",
          "9 to 15 April",
          "1 to 8 April"
        )

        actualDutyDefermentStatementGroups mustBe expectedDutyDefermentStatementGroups
      }
    }
  }

  "requested statements page" should {
    "return OK" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  "redirect to an error page" when {
    "the document service throws an exception" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      when(mockDocumentService.getDutyDefermentStatements(
        eqTo(newUser().eori),
        eqTo(someDan))(any))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      when(mockApiService.deleteNotification(eqTo(newUser().eori), any)(any)).thenReturn(Future.successful(true))

      val historicEoris = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

      val appWithEoriHistory = application(historicEoris).overrides(
        bind[ApiService].toInstance(mockApiService),
        bind[DataStoreService].toInstance(mockDataStoreService),
        bind[DocumentService].toInstance(mockDocumentService),
        bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      ).build()

      running(appWithEoriHistory) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(appWithEoriHistory, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must include("/duty-deferment/link_id/statements-unavailable")
      }
    }
  }

  "show unauthorised page" when {
    "sessionId is missing from the request" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.showAccountDetails(linkId).url)
        val result = route(app, request).value
        status(result) mustBe UNAUTHORIZED
      }
    }
  }

  "statementsUnavailablePage " should {
    "return 200" in new Setup {
      when(mockSessionCacheConnector.retrieveSession(eqTo(sessionId), eqTo(linkId))(any))
        .thenReturn(Future.successful(Some(AccountLink(sessionId, newUser().eori, someDan, AccountStatusOpen, Option(DefermentAccountAvailable), "", DateTime.now))))

      running(app) {
        val request = fakeRequest(GET, routes.DutyDefermentAccountController.statementsUnavailablePage(linkId).url).withHeaders("X-Session-Id" -> sessionId)
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }
  }

  trait Setup {
    val linkId = "link_id"
    val someDan = "1234567"
    val toOptionalDate: String => Option[LocalDate] = in => Some(LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))
    val sessionId = "session_1234"

    val dutyDefermentStatementFile1: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_04_01-08.pdf", "url.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
    val dutyDefermentStatementFile2: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_04_01-08.csv", "url.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 8, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
    val dutyDefermentStatementFile3: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_04_09-15.pdf", "url2.pdf", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Pdf, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
    val dutyDefermentStatementFile4: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_04_09-15.csv", "url2.csv", 1024L, DutyDefermentStatementFileMetadata(2018, 4, 9, 2018, 4, 15, Csv, DutyDefermentStatement, Weekly, Some(true), Some("BACS"), someDan, None))
    val sdesFileWithMetaDataCacheIdList = Seq(
      dutyDefermentStatementFile1,
      dutyDefermentStatementFile2,
      dutyDefermentStatementFile3,
      dutyDefermentStatementFile4)

    val dutyDefermentStatementFileWithSupplementary: DutyDefermentStatementFile = DutyDefermentStatementFile("2018_04_01-30.pdf", "url3.pdf", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 30, Pdf, DutyDefermentStatement, Supplementary, Some(true), Some("BACS"), someDan, None))
    val sdesFileWithMetaDataCacheIdSupplementaryList = sdesFileWithMetaDataCacheIdList ++ List(dutyDefermentStatementFileWithSupplementary)

    val dutyDefermentStatementFileWithExcise = DutyDefermentStatementFile("2018_04_01-30.pdf", "url3.pdf", 2048L, DutyDefermentStatementFileMetadata(2018, 4, 1, 2018, 4, 30, Pdf, DutyDefermentStatement, Excise, Some(true), Some("BACS"), someDan, None))

    val sdesFileWithMetaDataCacheIdExciseList = sdesFileWithMetaDataCacheIdList ++ List(dutyDefermentStatementFileWithExcise)

    val sdesFileWithMetaDataCacheIdExciseAndSupplementaryList = sdesFileWithMetaDataCacheIdList ++ sdesFileWithMetaDataCacheIdExciseList ++ sdesFileWithMetaDataCacheIdSupplementaryList

    val mockApiService = mock[ApiService]
    val mockDataStoreService = mock[DataStoreService]
    val mockDocumentService = mock[DocumentService]
    val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    val app = application().overrides(
      bind[ApiService].toInstance(mockApiService),
      bind[DataStoreService].toInstance(mockDataStoreService),
      bind[DocumentService].toInstance(mockDocumentService),
      bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
    ).build()
  }

}

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
import play.api.inject
import play.api.test.Helpers._
import uk.gov.hmrc.customs.financials.domain.FileFormat.Pdf
import uk.gov.hmrc.customs.financials.domain.FileRole.SecurityStatement
import uk.gov.hmrc.customs.financials.domain._
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.customs.financials.utils.SpecBase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class SecuritiesControllerSpec extends SpecBase {

  "The Security Statements Page" should {

    "return OK" in new Setup {
      when(mockApiService.deleteNotification(any, any)(any))
        .thenReturn(Future.successful(true))
      running(app) {
        val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
        val result = route(app, request).value
        status(result) mustBe OK
      }
    }

    "inform API to remove Security Statement notifications" in new Setup {
      when(mockApiService.deleteNotification(any, any)(any))
        .thenReturn(Future.successful(true))

      running(app) {
        val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
        val result = route(app, request).value
        status(result) mustBe OK
        verify(mockApiService).deleteNotification(eqTo(newUser().eori), eqTo(SecurityStatement))(any)
      }
    }

    "summary section" should {
      "have the back link" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.getElementsByClass("link-back").attr("href") mustBe "/customs/payment-records"
        }
      }

      "have heading 'Notification of adjustment statements'" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.getElementsByTag("h1").text mustBe s"Notification of adjustment statements"
        }
      }

      "display a notification link to requested statements when available" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))
        when(mockDocumentService.getSecurityStatements(eqTo(newUser().eori))(any))
          .thenReturn(Future.successful(sdesFileWithMetaDataCacheIdListWithRequestId))


        val appWithEoriHistory = application(Seq(EoriHistory("testEori1", toOptionalDate("1987-03-20"), None))).overrides(
          inject.bind[SdesService].toInstance(mockSdesService),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService)
        ).build()
        running(appWithEoriHistory) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(appWithEoriHistory, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.containsElementById("request-statement-link")
        }
      }

      "not display a notification link to requested statements when not available" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))
        when(mockDocumentService.getSecurityStatements(eqTo(newUser().eori))(any))
          .thenReturn(Future.successful(sdesFileWithMetaDataCacheIdListWithRequestId))


        val appWithEoriHistory = application(Seq(EoriHistory("testEori1", toOptionalDate("1987-03-20"), None))).overrides(
          inject.bind[SdesService].toInstance(mockSdesService),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService)
        ).build()
        running(appWithEoriHistory) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(appWithEoriHistory, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.containsElementById("request-statement-link")
        }
      }

      "hide link to historic statements request when the feature is disabled" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.containsLink("/customs/payment-records")
        }
      }

      "display link to historic statements request when the feature is enabled" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.containsLink("/customs/payment-records/historic-request")
        }
      }

      "display No statements to download, when user has no statements" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.containsElementById("no-statements")
        }
      }
    }

    "display Security statements, which" should {
      "not display blank sections for historic eoris that have no statements" in new Setup {
        val historicEori = "GBHistoricEori1"

        when(mockDocumentService.getSecurityStatements(eqTo(newUser().eori))(any))
          .thenReturn(Future.successful(sdesFileWithMetaDataCacheIdList))
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))
        when(mockDocumentService.getSecurityStatements(eqTo(historicEori))(any))
          .thenReturn(Future.successful(Seq.empty))
        val eoriHistory = Seq(EoriHistory(historicEori, toOptionalDate("1987-03-20"), toOptionalDate("2018-02-01")), EoriHistory(newUser().eori, toOptionalDate("2018-03-01"), None))
        val appWithEoriHistory = application(eoriHistory).overrides(
          inject.bind[SdesService].toInstance(mockSdesService),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService)
        ).build()
        running(appWithEoriHistory) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(appWithEoriHistory, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))
          html.containsElementById("statements-list-0")
          html.notContainElementById("statements-list-1")
        }
      }
    }

    "have a footer section" which {
      "displays the can`t find what you`re looking for message" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.containsElementById("missing-documents-guidance-heading")
          html.containsElementById("missing-documents-guidance-text1")
          html.containsElementById("missing-documents-guidance-text2")
        }

      }

      "display 'Help and support' section" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.select("#help-message")
          html.getElementsByTag("h2").get(1).text mustBe "Help and support"
        }
      }

      "displays link to historic statements request when the feature is enabled" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))

        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(app, request).value
          status(result) mustBe OK
          val html = Jsoup.parse(contentAsString(result))

          html.containsElementById("historic-statement-request")
        }
      }
    }

    "redirect to an error page" when {
      "the document service throws an exception" in new Setup {
        when(mockApiService.deleteNotification(any, any)(any))
          .thenReturn(Future.successful(true))
        val eoriHistory = Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))

        when(mockDocumentService.getSecurityStatements(eqTo(newUser().eori))(any))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val appWithEoriHistory = application(eoriHistory).overrides(
          inject.bind[SdesService].toInstance(mockSdesService),
          inject.bind[ApiService].toInstance(mockApiService),
          inject.bind[DocumentService].toInstance(mockDocumentService),
          inject.bind[DataStoreService].toInstance(mockDataStoreService)
        ).build()
        running(appWithEoriHistory) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url).withHeaders("X-Session-Id" -> "someSessionId")
          val result = route(appWithEoriHistory, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe "/customs/payment-records/adjustments/statements-unavailable"
        }
      }

      "show unauthorized page when session id is missing from the request" in new Setup {
        running(app) {
          val request = fakeRequest(GET, routes.SecuritiesController.showSecurityStatements().url)
          val result = route(app, request).value
          status(result) mustBe UNAUTHORIZED
        }
      }
    }
  }

  trait Setup {
    val mockSdesService: SdesService = mock[SdesService]
    val mockApiService: ApiService = mock[ApiService]
    val mockDocumentService = mock[DocumentService]
    val mockDataStoreService = mock[DataStoreService]

    val someEori = "12345678"
    val toOptionalDate: String => Option[LocalDate] = in => Some(LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))

    val securityStatementFile = SecurityStatementFile("statementfile_00", "download_url_00", 99L, SecurityStatementFileMetadata(2017, 12, 28, 2018, 1, 1, Pdf, SecurityStatement, someEori, 500L, "0000000", None)) // scalastyle:ignore magic.number
    val securityStatementFileWithRequestId = SecurityStatementFile("statementfile_02", "download_url_02", 222L, SecurityStatementFileMetadata(2018, 2, 1, 2018, 2, 28, Pdf, SecurityStatement, someEori, 2500L, "2123456", Some("statement-request-id"))) // scalastyle:ignore magic.number

    val sdesFileWithMetaDataCacheIdList = Seq(securityStatementFile)
    val sdesFileWithMetaDataCacheIdListWithRequestId = Seq(securityStatementFileWithRequestId)

    val app = application().overrides(
      inject.bind[SdesService].toInstance(mockSdesService),
      inject.bind[ApiService].toInstance(mockApiService),
      inject.bind[DocumentService].toInstance(mockDocumentService),
      inject.bind[DataStoreService].toInstance(mockDataStoreService)
    ).build()
  }

}
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

package controllers

import domain.FileFormat.Pdf
import domain.FileRole.C79Certificate
import domain.{EoriHistory, VatCertificateFile, VatCertificateFileMetadata}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Messages
import play.api.inject
import play.api.test.Helpers._
import services.{ApiService, DataStoreService, DocumentService}
import utils.SpecBase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class VatControllerSpec extends SpecBase {

  "VatController.showVatAccount" should {

    "return OK for 'import vat page'" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.VatController.showVatAccount.url).withHeaders("X-Session-Id" -> "someSession")
        val result = route(app, request).value
        status(result) mustBe OK
        verify(mockApiService).deleteNotification(eqTo(newUser().eori), eqTo(C79Certificate))(any)
      }
    }

    "redirect to an error page" when {
      "the document service throws an exception" in new Setup {

        when(mockDocumentService.getVatCertificates(eqTo(newUser().eori))(any, any))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        running(app) {
          val request = fakeRequest(GET, routes.VatController.showVatAccount.url).withHeaders("X-Session-Id" -> "someSession")
          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe "/customs/payment-records/import-vat/certificates-unavailable"
        }
      }
    }

    "display message when no certificates available" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.VatController.showVatAccount.url).withHeaders("X-Session-Id" -> "someSession")
        val result = route(app, request).value
        status(result) mustBe OK
        contentAsString(result) must include regex "There were no certificates in June"
      }
    }
  }

  trait Setup {
    implicit val messages: Messages = stubMessages()
    val toOptionalDate: String => Option[LocalDate] = in => Some(LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE))
    val vatCertificateFile: VatCertificateFile = VatCertificateFile("name_04", "download_url_06", 111L, VatCertificateFileMetadata(2018, 3, Pdf, C79Certificate, None), "")
    val sdesFileWithMetaDataCacheIdList = Seq(vatCertificateFile)

    val mockApiService: ApiService = mock[ApiService]
    val mockDocumentService = mock[DocumentService]
    val mockDataStoreService = mock[DataStoreService]


    val app = application(Seq(EoriHistory(newUser().eori, toOptionalDate("1987-03-20"), None))).overrides(
      inject.bind[ApiService].toInstance(mockApiService),
      inject.bind[DocumentService].toInstance(mockDocumentService),
      inject.bind[DataStoreService].toInstance(mockDataStoreService)
    ).build()

    when(mockApiService.deleteNotification(any, any)(any)).thenReturn(Future.successful(true))

    when(mockDocumentService.getVatCertificates(eqTo("testEori1"))(any,any))
      .thenReturn(Future.successful(sdesFileWithMetaDataCacheIdList))
  }
}
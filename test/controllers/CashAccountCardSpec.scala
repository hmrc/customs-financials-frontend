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

package controllers

import config.AppConfig
import connectors.CustomsFinancialsSessionCacheConnector
import domain.{
  AccountStatusOpen, CDSAccounts, CDSCashBalance, CashAccount, DefermentAccountAvailable, XiEoriAddressInformation
}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.{Application, inject}
import play.api.test.Helpers._
import services.{ApiService, DataStoreService, NotificationService, XiEoriInformationReponse}
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.http.HttpResponse
import utils.SpecBase
import utils.MustMatchers

import scala.concurrent.Future

class CashAccountCardSpec extends SpecBase with MustMatchers {

  "the landing page" should {
    "show cash account card details" in new Setup {
      running(app) {
        val result = route(app, request).value
        val html   = Jsoup.parse(contentAsString(result))

        val expectedUrl = appConfig.cashAccountUrl
        html.containsLink(expectedUrl)
        html.getElementsByClass("available-account-balance").text mustBe "£98,765 available"
      }
    }

    "should render correct ID" in new Setup {
      running(app) {
        val result = route(app, request).value
        val html   = Jsoup.parse(contentAsString(result))

        html
          .getElementById(s"cash-account-$someCashAccountNumber")
          .attr("id") mustBe "cash-account-123456789"
      }
    }
  }

  trait Setup {
    val someCashAccountNumber        = "123456789"
    val someAvailableCashBalance     = 98765
    val someCashAccount: CashAccount = CashAccount(
      someCashAccountNumber,
      newUser().eori,
      AccountStatusOpen,
      DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(someAvailableCashBalance)))
    )

    val add: XiEoriAddressInformation =
      XiEoriAddressInformation(emptyString, Some(emptyString), None, None, Some(emptyString))

    val xi: XiEoriInformationReponse = XiEoriInformationReponse("SomeXiEori", "yes", add)

    val mockAccounts: CDSAccounts                                         = mock[CDSAccounts]
    val mockApiService: ApiService                                        = mock[ApiService]
    val mockNotificationService: NotificationService                      = mock[NotificationService]
    val mockDataStoreService: DataStoreService                            = mock[DataStoreService]
    val mockSessionCacheConnector: CustomsFinancialsSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    val app: Application = application()
      .overrides(
        inject.bind[CDSAccounts].toInstance(mockAccounts),
        inject.bind[ApiService].toInstance(mockApiService),
        inject.bind[NotificationService].toInstance(mockNotificationService),
        inject.bind[DataStoreService].toInstance(mockDataStoreService),
        inject.bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
      )
      .build()

    val appConfig: AppConfig                         = app.injector.instanceOf[AppConfig]
    val request: FakeRequest[AnyContentAsEmpty.type] =
      fakeRequest(GET, routes.CustomsFinancialsHomeController.index.url).withHeaders("X-Session-Id" -> "session-1234")

    when(mockAccounts.myAccounts).thenReturn(List(someCashAccount))
    when(mockAccounts.accounts).thenReturn(List(someCashAccount))
    when(mockAccounts.isAgent).thenReturn(false)
    when(mockAccounts.isNiAccount).thenReturn(Some(false))

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(mockAccounts))

    when(mockNotificationService.fetchNotifications(ArgumentMatchers.any()))
      .thenReturn(Future.successful(List()))

    when(mockDataStoreService.getEmail(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))

    when(mockSessionCacheConnector.storeSession(any, any)(any))
      .thenReturn(Future.successful(HttpResponse(OK, emptyString)))

    when(mockDataStoreService.getCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Company Name")))
    when(mockDataStoreService.getOwnCompanyName(any)).thenReturn(Future.successful(Some("Test Own Company Name")))
    when(mockDataStoreService.getXiEori(any)).thenReturn(Future.successful(None))
  }
}

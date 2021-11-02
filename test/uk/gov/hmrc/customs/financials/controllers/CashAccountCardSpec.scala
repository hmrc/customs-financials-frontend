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
import play.api.http.Status
import play.api.inject.bind
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.connectors.CustomsFinancialsSessionCacheConnector
import uk.gov.hmrc.customs.financials.domain.{DefermentAccountAvailable, _}
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.http.HttpResponse
import scala.concurrent.Future

class CashAccountCardSpec extends SpecBase {

  "the landing page" should {
    "show cash account card details" in new Setup {
      running(app) {
        val request = fakeRequest(GET, routes.CustomsFinancialsHomeController.index().url).withHeaders("X-Session-Id" -> "session-1234")
        val result = route(app, request).value
        val html = Jsoup.parse(contentAsString(result))

        val appConfig = app.injector.instanceOf[AppConfig]

        val expectedUrl = appConfig.cashAccountUrl
        html.containsLink(expectedUrl)
        html.getElementsByClass("available-account-balance").text mustBe "£98,765 available"
      }
    }
  }

  trait Setup {
    val someCashAccountNumber = "123456789"
    val someAvailableCashBalance = 98765
    val someCashAccount = CashAccount(
      someCashAccountNumber,
      newUser().eori,
      AccountStatusOpen,
      DefermentAccountAvailable,
      CDSCashBalance(Some(BigDecimal(someAvailableCashBalance)))
    )
    val mockAccounts = mock[CDSAccounts]
    val mockApiService = mock[ApiService]
    val mockNotificationService = mock[NotificationService]
    val mockDataStoreService = mock[DataStoreService]
    val mockSessionCacheConnector = mock[CustomsFinancialsSessionCacheConnector]

    val app = application().overrides(
      bind[CDSAccounts].toInstance(mockAccounts),
      bind[ApiService].toInstance(mockApiService),
      bind[NotificationService].toInstance(mockNotificationService),
      bind[DataStoreService].toInstance(mockDataStoreService),
      bind[CustomsFinancialsSessionCacheConnector].toInstance(mockSessionCacheConnector)
    ).build()

    when(mockAccounts.myAccounts).thenReturn(List(someCashAccount))
    when(mockAccounts.accounts).thenReturn(List(someCashAccount))
    when(mockAccounts.eori).isLenient()
    when(mockAccounts.isAgent).thenReturn(false)

    when(mockApiService.getAccounts(ArgumentMatchers.eq(newUser().eori))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(mockAccounts))
    when(mockNotificationService.fetchNotifications(ArgumentMatchers.eq(newUser().eori))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(List()))
    when(mockDataStoreService.getEmail(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(Email("last.man@standing.co.uk"))))
    when(mockSessionCacheConnector.storeSession(any, any)(any)).thenReturn(Future.successful(HttpResponse(Status.OK, "")))
    when(mockDataStoreService.getCompanyName(any)(any)).thenReturn(Future.successful(Some("Test Company Name")))

  }
}

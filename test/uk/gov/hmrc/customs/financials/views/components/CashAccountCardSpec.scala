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

package uk.gov.hmrc.customs.financials.views.components

import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
config.AppConfig
domain._
utils.SpecBase
views.html.components.cash_account_card

class CashAccountCardSpec extends SpecBase {

  "Cash Account Card" should {


    "be marked with class 'cash-account'" in new Setup {
      running(app) {
        content(cashAccount).getElementsByClass("cash-account").isEmpty mustBe false
      }
    }

    "be marked with class 'card-header'" in new Setup {
      running(app) {
        content(cashAccount).getElementsByClass("card-header").isEmpty mustBe false
      }
    }

    "include a link to the new cash account service" in new Setup {
      running(app) {
        content(cashAccount).containsLinkWithText("http://localhost:9394/customs/cash-account", "View account")
      }
    }

    "include account balance" in new Setup {
      running(app) {
        content(cashAccount).getElementsByTag("p").hasClass("available-account-balance") mustBe true
      }
    }

    "display suspended when the account has been suspended" in new Setup {
      running(app) {
        val newCashAccount = CashAccount("123456", "owner", AccountStatusSuspended, DirectDebitMandateCancelled, CDSCashBalance(Some(BigDecimal(987))))
        content(newCashAccount).getElementsByClass("cash-account-status").hasClass("account-status-suspended") mustBe true
      }
    }

    "generate a hidden suspended status for screen readers" in new Setup {
      running(app) {
        val newCashAccount = CashAccount("123456", "owner", AccountStatusSuspended, DirectDebitMandateCancelled, CDSCashBalance(Some(BigDecimal(987))))
        val status = content(newCashAccount).select("h4.cash-account-status").first

        status.getElementsByTag("span").hasClass("govuk-visually-hidden") mustBe true
      }
    }

    "display closed when the account has been closed" in new Setup {
      running(app) {
        val newCashAccount = CashAccount("123456", "owner", AccountStatusClosed, AccountCancelled, CDSCashBalance(Some(BigDecimal(876))))
        content(newCashAccount).getElementsByClass("cash-account-status").hasClass("account-status-closed") mustBe true
      }
    }

    "generate a hidden closed status for screen readers" in new Setup {
      running(app) {
        val newCashAccount = CashAccount("123456", "owner", AccountStatusClosed, AccountCancelled, CDSCashBalance(Some(BigDecimal(876))))
        val status = content(newCashAccount).select("h4.cash-account-status").first
        status.getElementsByTag("span").hasClass("govuk-visually-hidden") mustBe true
      }
    }

    "include a top up link" in new Setup {
      running(app) {
        content(cashAccount).containsLinkWithText("https://www.gov.uk/guidance/paying-into-your-cash-account-for-cds-declarations", "Top up")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")
    val app = application().build()
    implicit val appConfig = app.injector.instanceOf[AppConfig]
    val cashAccount = CashAccount("123456", "owner", AccountStatusOpen, DefermentAccountAvailable, CDSCashBalance(Some(BigDecimal(999))))
    def content(cashAccount: CashAccount) = Jsoup.parse(app.injector.instanceOf[cash_account_card].apply(cashAccount).body)



    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

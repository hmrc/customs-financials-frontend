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

package views.components

import config.AppConfig
import domain.{
  AccountCancelled, AccountStatusClosed, AccountStatusOpen, AccountStatusSuspended,
  DefermentAccountAvailable, DirectDebitMandateCancelled, GeneralGuaranteeAccount, GeneralGuaranteeBalance
}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import utils.SpecBase
import utils.TestData.{BALANCE_499, BALANCE_999}
import views.html.account_cards.guarantee_account_cards

class GuaranteeAccountCardSpec extends SpecBase {

  "Guarantee Account Card" should {

    "be marked with class 'guarantee-account'" in new Setup {
      running(app) {
        content().getElementsByClass("guarantee-account").isEmpty mustBe false
      }
    }

    "be marked with class 'card-header'" in new Setup {
      running(app) {
        content().getElementsByClass("card-header").isEmpty mustBe false
      }
    }

    "include a link to the guarantee account details page when guarantee account details" in new Setup {
      running(app) {
        content().getElementsByTag("a").text mustBe "View general guarantee account"
        content().getElementsByTag("a")
          .attr("href") mustBe "http://localhost:9395/customs/guarantee-account"
      }
    }

    "include guarantee limit remaining" in new Setup {
      running(app) {
        content().getElementsByClass("guarantee-account-limit-remaining").text mustBe "£499 available"
      }
    }

    "include overall guarantee limit" in new Setup {
      running(app) {
        content().getElementById("guarantee-limit-123456").text mustBe "Guarantee limit: £999"
      }
    }
  }

  "Guarantee Account Card with no gurantee balances" should {
    val newGuaranteeAccount = GeneralGuaranteeAccount("123456",
      "owner",
      AccountStatusOpen,
      DefermentAccountAvailable,
      None)

    "include guarantee limit remaining" in new Setup {
      running(app) {
        content(newGuaranteeAccount).getElementsByClass("guarantee-account-limit-remaining").text mustBe "£0"
      }
    }

    "not include overall guarantee limit when value is 0" in new Setup {
      running(app) {
        content(newGuaranteeAccount).notContainElementById("overall-guarantee-limit")
      }
    }

    "account is open" should {
      val newGuaranteeAccount = GeneralGuaranteeAccount(
        "123456",
        "owner",
        AccountStatusOpen,
        DefermentAccountAvailable,
        Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_999), BigDecimal(BALANCE_499))))

      "not display open account status" in new Setup {
        running(app) {
          content(newGuaranteeAccount).getElementsByClass("guarantee-account-status").isEmpty mustBe true
        }
      }
    }

    "account is suspended" should {
      val newGuaranteeAccount = GeneralGuaranteeAccount(
        "123456",
        "owner",
        AccountStatusSuspended,
        DirectDebitMandateCancelled,
        Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_999), BigDecimal(BALANCE_499))))

      "hidden status-description for screen readers" in new Setup {
        running(app) {
          val status = content(newGuaranteeAccount).select(".guarantee-account").first
          status.getElementsByTag("span").first
            .getElementsByClass("govuk-visually-hidden").isEmpty mustBe false
        }
      }

      "not display limit bar" in new Setup {
        running(app) {
          content(newGuaranteeAccount).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }
    }

    "account is closed" should {
      val newGuaranteeAccount = GeneralGuaranteeAccount("123456",
        "owner",
        AccountStatusClosed,
        AccountCancelled,
        Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_999), BigDecimal(BALANCE_499))))

      "not display limit bar" in new Setup {
        running(app) {
          content(newGuaranteeAccount).getElementsByClass("progress-bar").isEmpty mustBe true
        }
      }

      "hidden status-description for screen readers" in new Setup {
        running(app) {
          val status = content(newGuaranteeAccount).select(".guarantee-account").first
          status.getElementsByTag("span").first().hasClass("govuk-visually-hidden") mustBe true
        }
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val csrfRequest: FakeRequest[AnyContentAsEmpty.type] =
      fakeRequest("GET", "/some/resource/path")

    val app: Application = application().build()
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

    val guaranteeAccount: GeneralGuaranteeAccount = GeneralGuaranteeAccount("123456",
      "owner",
      AccountStatusOpen,
      DefermentAccountAvailable,
      Some(GeneralGuaranteeBalance(BigDecimal(BALANCE_999), BigDecimal(BALANCE_499))))

    def content(guaranteeAccount: GeneralGuaranteeAccount = guaranteeAccount): Document =
      Jsoup.parse(app.injector.instanceOf[guarantee_account_cards].apply(Seq(guaranteeAccount)).body)
  }
}

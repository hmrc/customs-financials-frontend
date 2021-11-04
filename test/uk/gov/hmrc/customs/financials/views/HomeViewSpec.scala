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

import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.customs.financials.config.AppConfig
import uk.gov.hmrc.customs.financials.domain.{AccountLink, AccountStatusOpen, CDSAccounts, DefermentAccountAvailable, DutyDefermentAccount, DutyDefermentBalance}
import uk.gov.hmrc.customs.financials.utils.SpecBase
import uk.gov.hmrc.customs.financials.viewmodels.FinancialsHomeModel
import uk.gov.hmrc.customs.financials.views.html.customs_financials_home
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.util.Random

class HomeViewSpec extends SpecBase {

  "Customs Financials Home View" should {
    "display header as a link text" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsLinkWithText("#", "View your customs financial accounts")
      }
    }

    "display other accounts you can use link" when {
      "agent has access to other accounts with AuthorisedToView" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsElementById("authorised-to-view-link")
        }
      }
    }

    "display manage your account authorities link" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsElementById("manage-account-authorities-link")
      }
    }
  }

  "not display manage your account authorities link" when {
    "display help and support message" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsElementById("help_and_support")
      }
    }

    "display recruitment banner" when {
      "display duty deferment inaccurate balance message" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsElementById("duty-deferment-balances-warning")
        }
      }
    }

    "display the message banner partial" in new Setup {
      private val bannerHtmlPartial = HtmlPartial.Success(None, Html("<b id='banner-html'>Banner html</b>"))
      running(app) {
        page(modelWithAgentAccess, Some(bannerHtmlPartial.content)).containsElementById("banner-html")
      }
    }

    "display the EORI and company name in the banner" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsElementById("eori-company")
      }
    }

    "display only EORI in banner when none returned for company name" in new Setup {
      override val modelWithAgentAccess = FinancialsHomeModel(eori, None, accounts, Nil, accountLinks)
      running(app) {
        page(modelWithAgentAccess, None).containsElementById("eori")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request = FakeRequest("GET", "/some/resource/path")

    val app = application().build()

    implicit val appConfig = app.injector.instanceOf[AppConfig]

    val companyName = Some("Company Name 1")
    val eori = "EORI0123"
    val eori1 = "EORI01234"
    val dan1 = "DAN01234"
    val dan2 = "DAN43210"

    def randomFloat: Float = Random.nextFloat()

    def randomBigDecimal: BigDecimal = BigDecimal(randomFloat.toString)

    val accounts: Seq[CDSAccounts] = Seq(
      CDSAccounts(eori, Seq(DutyDefermentAccount(dan1, eori, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)), viewBalanceIsGranted = true, isIsleOfMan = false),
        DutyDefermentAccount(dan2, eori1, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)), viewBalanceIsGranted = true, isIsleOfMan = false)))
    )

    val accountsWithNoAgent: Seq[CDSAccounts] = Seq(
      CDSAccounts(eori, Seq(DutyDefermentAccount(dan1, eori, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)), viewBalanceIsGranted = true, isIsleOfMan = false),
        DutyDefermentAccount(dan2, eori, AccountStatusOpen, DefermentAccountAvailable, DutyDefermentBalance(Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal), Some(randomBigDecimal)), viewBalanceIsGranted = true, isIsleOfMan = false)))
    )

    val accountLinks = Seq(AccountLink(sessionId = "sessionId", eori, accountNumber = dan1, linkId = "linkId", accountStatus = AccountStatusOpen, accountStatusId = Option(DefermentAccountAvailable), lastUpdated = DateTime.now()))

    val modelWithAgentAccess = FinancialsHomeModel(eori, companyName, accounts, Nil, accountLinks)

    def page(viewModel: FinancialsHomeModel, maybeBannerPartial: Option[HtmlFormat.Appendable]) = Jsoup.parse(app.injector.instanceOf[customs_financials_home].apply(viewModel, maybeBannerPartial).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

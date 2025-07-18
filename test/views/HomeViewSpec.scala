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

package views

import config.AppConfig
import domain.{
  AccountLink, AccountStatusOpen, CDSAccounts, DefermentAccountAvailable, DutyDefermentAccount, DutyDefermentBalance
}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.SpecBase
import viewmodels.FinancialsHomeModel
import views.html.dashboard.customs_financials_home

import java.time.LocalDateTime
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
          page(modelWithAgentAccess, None).containsLink("/customs/payment-records/authorized-to-view?page=1")
        }
      }
    }

    "display manage your account authorities link" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsLink("http://localhost:9000/customs/manage-authorities")
      }
    }
  }

  "not display manage your account authorities link" when {

    "banner links" when {

      "display the message banner partial" in new Setup {
        private val bannerHtmlPartial = HtmlPartial.Success(None, Html("<b id='banner-html'>Banner html</b>"))

        running(app) {
          page(modelWithAgentAccess, Some(bannerHtmlPartial.content)).containsElementById("banner-html")
        }
      }

      "displays home as a link text" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsLinkWithText("#", "Home")
        }
      }

      "displays Messages as a link text" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsLinkWithText("#", "Messages")
        }
      }

      "displays Your contact details as a link text" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsLinkWithText("#", "Your contact details")
        }
      }

      "displays Your account authorities as a link text" in new Setup {
        running(app) {
          page(modelWithAgentAccess, None).containsLinkWithText("#", "Your account authorities")
        }
      }
    }

    "display the EORI and company name in the banner" in new Setup {
      running(app) {
        page(modelWithAgentAccess, None).containsElementById("eori-company")
      }
    }

    "display only EORI in banner when none returned for company name" in new Setup {
      override val modelWithAgentAccess: FinancialsHomeModel =
        FinancialsHomeModel(eori, None, accounts, Nil, accountLinks)

      running(app) {
        page(modelWithAgentAccess, None).containsElementById("eori")
      }
    }
  }

  trait Setup extends I18nSupport {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val app: Application = application().build()

    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

    val companyName: Option[String] = Some("Company Name 1")
    val eori                        = "EORI0123"
    val eori1                       = "EORI01234"
    val dan1                        = "DAN01234"
    val dan2                        = "DAN43210"

    def randomFloat: Float = Random.nextFloat()

    def randomBigDecimal: BigDecimal = BigDecimal(randomFloat.toString)

    val accounts: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eori,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eori,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          ),
          DutyDefermentAccount(
            dan2,
            eori1,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val accountsWithNoAgent: Seq[CDSAccounts] = Seq(
      CDSAccounts(
        eori,
        None,
        Seq(
          DutyDefermentAccount(
            dan1,
            eori,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          ),
          DutyDefermentAccount(
            dan2,
            eori,
            isNiAccount = false,
            AccountStatusOpen,
            DefermentAccountAvailable,
            DutyDefermentBalance(
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal),
              Some(randomBigDecimal)
            ),
            viewBalanceIsGranted = true,
            isIsleOfMan = false
          )
        )
      )
    )

    val accountLinks: Seq[AccountLink] = Seq(
      AccountLink(
        sessionId = "sessionId",
        eori,
        isNiAccount = false,
        accountNumber = dan1,
        linkId = "linkId",
        accountStatus = AccountStatusOpen,
        accountStatusId = Option(DefermentAccountAvailable),
        lastUpdated = LocalDateTime.now()
      )
    )

    val modelWithAgentAccess: FinancialsHomeModel = FinancialsHomeModel(eori, companyName, accounts, Nil, accountLinks)

    def page(viewModel: FinancialsHomeModel, maybeBannerPartial: Option[HtmlFormat.Appendable]): Document =
      Jsoup.parse(app.injector.instanceOf[customs_financials_home].apply(viewModel, maybeBannerPartial).body)

    override def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  }
}

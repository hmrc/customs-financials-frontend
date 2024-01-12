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

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.Helpers
import utils.SpecBase

import scala.collection.JavaConverters._

class NotificationPanelSpec extends SpecBase {
  implicit val messages = Helpers.stubMessages()

  "Notification Panel" should {
    "not be displayed when there are no messages" in {
      val messageKeys = List()
      val content: Element = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)
      content.getElementById("notification-panel") mustBe null
    }

    "display C79 certificate notification" when {
      "new C79 certificate is available" in {
        val messageKeys = List("c79")
        val expected = List("cf.customs-financials-home.notification.c79")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        expected.map(message =>
          content.select("#notification-panel p").asScala.map(_.text()).toList must contain(message)
        )
      }

      "new requested C79 certificate is available" in {
        val messageKeys = List("requested-c79")
        val expected = List("cf.customs-financials-home.notification.requested-c79")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        expected.map(message =>
          content.select("#notification-panel p").asScala.map(_.text()).toList must contain(message)
        )
      }
    }

    "not display C79 certificate notification" when {

      "there are no new C79 certificates" in {
        val messageKeys = List()
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        content.getElementsContainingText("You have a new Import VAT (C79) certificate").isEmpty mustBe true
        content.getElementsContainingText("Your requested import VAT certificates (C79) are available to view").isEmpty mustBe true
      }

    }

    "display duty deferment statement notification" when {
      "new duty deferment statement is available" in {
        val messageKeys = List("duty-deferment")
        val expected = List("cf.customs-financials-home.notification.duty-deferment")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        expected.map(message =>
          content.select("#notification-panel p").asScala.map(_.text()).toList must contain(message)
        )
      }

      "new requested duty deferment statement is available " in {
        val messageKeys = List("cf.customs-financials-home.notification.requested-duty-deferment")
        val expected = List("cf.customs-financials-home.notification.cf.customs-financials-home.notification.requested-duty-deferment")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        expected.map(message =>
          content.select("#notification-panel p").asScala.map(_.text()).toList must contain(message)
        )
      }
    }

    "not display duty deferment statement notification" when {
      "there are no new duty deferment statements" in {
        val messageKeys = List()
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        content.getElementsContainingText("You have a new Duty Deferment statement").isEmpty mustBe true
        content.getElementsContainingText("Your requested Duty Deferment statements are available to view").isEmpty mustBe true
      }

    }

    "display Postponed VAT notification" when {
      "new Postponed VAT Statement is available" in {
        val messageKeys = List("cf.customs-financials-home.notification.postponed-vat")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)
        val expected = List("cf.customs-financials-home.notification.cf.customs-financials-home.notification.postponed-vat")

        content.select("#notification-panel p").asScala.map(_.text()).toList mustBe expected
      }
    }

    "not display Postponed VAT notification" when {
      "there are no new Postponed VAT statement" in {
        val messageKeys = List()
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        content.getElementsContainingText("You have a new Postponed import VAT statement").isEmpty mustBe true
      }
    }

    "display Securities notification" when {
      "new Securities statement is available" in {
        val messageKeys = List("adjustments")
        val expected = List("cf.customs-financials-home.notification.adjustments")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        content.select("#notification-panel p").asScala.map(_.text()).toList mustBe expected
      }

      "new requested Securities statement is available" in {
        val messageKeys = List("requested-adjustments")
        val expected = List("cf.customs-financials-home.notification.requested-adjustments")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)

        expected.map(message =>
          content.select("#notification-panel p").asScala.map(_.text()).toList must contain(message)
        )
      }
    }

    "not display Securities notification" when {
      "there is no new Securities statement" in {
        val messageKeys = List()
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)
        content.getElementsContainingText("cf.customs-financials-home.notification.adjustments").isEmpty mustBe true
      }
    }

    "display Standing Authorities notification" when {
      "new Standing authorities csv file is available" in {
        val messageKeys = List("authorities")
        val expected = List("cf.customs-financials-home.notification.authorities")
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)
        content.select("#notification-panel p").asScala.map(_.text()).toList mustBe expected
      }
    }

    "not display Standing Authorities notification" when {
      "there is no new Standing authorities csv file" in {
        val messageKeys = List()
        val content = Jsoup.parse(views.html.components.notification_panel(messageKeys).body)
        content.getElementsContainingText("cf.customs-financials-home.notification.authorities").isEmpty mustBe true
      }
    }
  }
}

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

package uk.gov.hmrc.customs.financials.utils

import akka.stream.testkit.NoMaterializer
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.jsoup.nodes.Document
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, OptionValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.stubPlayBodyParsers
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.customs.financials.actionbuilders.{FakeIdentifierAction, IdentifierAction}
import uk.gov.hmrc.customs.financials.domain.{EoriHistory, SignedInUser}

import scala.collection.JavaConverters.asScalaBufferConverter

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String = "{}"
}

trait SpecBase extends AnyWordSpecLike with MockitoSugar with OptionValues with ScalaFutures with Matchers with IntegrationPatience {

  implicit class DocumentHelper(document: Document) {
    def containsLink(link: String): Boolean = {
      val results = document.getElementsByTag("a").asScala.toList
      results.exists(_.attr("href") == link)
    }

    def containsLinkWithText(link: String, text: String): Boolean = {
      val results = document.getElementsByTag("a").asScala.toList
      val foundLinks = results.filter(_.attr("href") == link)
      if (foundLinks.nonEmpty){
        foundLinks.exists(_.text == text)
      } else false
    }

    def containsElementByIdWithText(id: String, text: String) = {
      val items = document.getElementsByAttribute("id").asScala.toList
      val filteredItems = items.filter(_.id() == id)
      assert(filteredItems.exists(_.text == text))
    }

    def containsElementById(id: String): Assertion = {
      assert(document.getElementsByAttribute("id").asScala.toList.exists(_.id() == id))
    }

    def notContainElementById(id: String): Assertion = {
      assert(!document.getElementsByAttribute("id").asScala.toList.exists(_.id() == id))
    }
  }

  def application(allEoriHistory: Seq[EoriHistory] = Seq.empty) = new GuiceApplicationBuilder().overrides(
    bind[IdentifierAction].toInstance(new FakeIdentifierAction(stubPlayBodyParsers(NoMaterializer))(allEoriHistory)),
    bind[Metrics].toInstance(new FakeMetrics)
  ).configure("auditing.enabled" -> "false")

  def fakeRequest(method: String = "", path: String = ""): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  def newUser(allEoriHistory: Seq[EoriHistory] = Seq.empty) = {
    val eori = "testEori4"
    SignedInUser(
      Some(Credentials("2345235235", "GovernmentGateway")),
      Some(Name(Some("firstName"), Some("secondName"))),
      Some("test@email.com"),
      "testEori1",
      Some(AffinityGroup.Individual),
      Some("Int-ba17b467-90f3-42b6-9570-73be7b78eb2b"),
      Enrolments(Set(
        Enrolment("IR-SA", List(EnrolmentIdentifier("UTR", "111111111")), "Activated", None),
        Enrolment("IR-CT", List(EnrolmentIdentifier("UTR", "222222222")), "Activated", None),
        Enrolment("HMRC-CUS-ORG", List(EnrolmentIdentifier("EORINumber", eori)), "Activated", None)
      )),
      allEoriHistory
    )
  }

}

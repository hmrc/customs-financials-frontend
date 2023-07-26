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

package utils


import actionbuilders.{FakeIdentifierAction, IdentifierAction}
import akka.stream.testkit.NoMaterializer
import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import domain.{EoriHistory, SignedInUser}
import org.jsoup.nodes.Document
import org.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{Assertion, OptionValues}
import play.api
import play.api.i18n.{Messages, MessagesApi}
import play.api.{Application, inject}
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.annotation.implicitNotFound
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.reflect.ClassTag

class FakeMetrics extends Metrics {
  override val defaultRegistry: MetricRegistry = new MetricRegistry
  override val toJson: String = "{}"
}

trait SpecBase extends AnyWordSpecLike
  with MockitoSugar
  with OptionValues
  with ScalaFutures
  with Matchers
  with IntegrationPatience {

  val emptyString = ""

  def messages(app: Application): Messages =
    app.injector.instanceOf[MessagesApi].preferred(fakeRequest(emptyString, emptyString))

  implicit class DocumentHelper(document: Document) {
    def containsLink(link: String): Boolean = {
      val results = document.getElementsByTag("a").asScala.toList
      results.exists(_.attr("href") == link)
    }

    def containsLinkWithText(link: String, text: String): Boolean = {
      val results = document.getElementsByTag("a").asScala.toList
      val foundLinks = results.filter(_.attr("href") == link)
      if (foundLinks.nonEmpty) foundLinks.exists(_.text == text) else false
    }

    def containsElementById(id: String): Assertion = {
      assert(document.getElementsByAttribute("id").asScala.toList.exists(_.id() == id))
    }

    def notContainElementById(id: String): Assertion = {
      assert(!document.getElementsByAttribute("id").asScala.toList.exists(_.id() == id))
    }
  }

  def application(allEoriHistory: Seq[EoriHistory] = Seq.empty): GuiceApplicationBuilder = new GuiceApplicationBuilder().overrides(
    inject.bind[IdentifierAction].toInstance(new FakeIdentifierAction(stubPlayBodyParsers(NoMaterializer))(allEoriHistory)),
    api.inject.bind[Metrics].toInstance(new FakeMetrics)
  ).configure("auditing.enabled" -> "false")

  @implicitNotFound("Pass a type for the identifier action")
  def applicationBuilder[IA <: IdentifierAction](disableAuth: Boolean = false)(implicit c: ClassTag[IA]) : GuiceApplicationBuilder = {

    val overrides: List[GuiceableModule] = List(bind[Metrics].toInstance(new FakeMetrics))
    val optionalOverrides: List[GuiceableModule] = if (disableAuth) {
      Nil
    } else {
      List(bind[IdentifierAction].to[IA])
    }

    new GuiceApplicationBuilder()
      .overrides(overrides ::: optionalOverrides: _*)
      .configure("play.filters.csp.nonce.enabled" -> false,
        "auditing.enabled" -> "false",
        "microservice.metrics.graphite.enabled" -> "false",
        "metrics.enabled" -> "false")
  }

  def fakeRequest(method: String = emptyString, path: String = emptyString): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  /**
   * Creates the FakeRequest with session with the supplied
   * method, path and sessionIdValue
   *
   * @param method HTTP Method type
   * @param path url for the Call
   * @param sessionIdValue Session value that is to be matched
   * @return
   */
  def fakeRequestWithSession(method: String = emptyString,
                             path: String = emptyString,
                             sessionIdValue: String = emptyString): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, path).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]
      .withSession(("sessionId", sessionIdValue))

  def newUser(allEoriHistory: Seq[EoriHistory] = Seq.empty): SignedInUser = {
    SignedInUser("testEori1", allEoriHistory, Some("someAltEori"))
  }
}

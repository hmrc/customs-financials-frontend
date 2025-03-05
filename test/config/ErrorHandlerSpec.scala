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

package config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.FakeRequest
import utils.SpecBase

class ErrorHandlerSpec extends AnyWordSpec with SpecBase with Matchers {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private val fakeRequest = FakeRequest("GET", "/")

  private val handler = application().build().injector.instanceOf[ErrorHandler]

  "Error handler" should {
    "render standard error template HTML" in {
      val html = handler.standardErrorTemplate("title", "heading", "message")(fakeRequest)

      html.map { htmlContent =>
        htmlContent.contentType shouldBe "text/html"
      }
    }

    "render not found template HTML" in {
      val html = handler.notFoundTemplate(fakeRequest)

      html.map { htmlContent =>
        htmlContent.contentType shouldBe "text/html"
      }
    }

    "render unauthorized HTML" in {
      val html = handler.unauthorized()(fakeRequest)
      html.contentType shouldBe "text/html"
    }

    "render technical difficulties HTML" in {
      val html = handler.technicalDifficulties()(fakeRequest)
      html.contentType shouldBe "text/html"
    }
  }
}

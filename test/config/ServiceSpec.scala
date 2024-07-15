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

import play.api.Configuration
import utils.{ShouldMatchers, SpecBase}
import org.scalatest.matchers.must.Matchers as MustMatchers

class ServiceSpec extends SpecBase with ShouldMatchers {

  "should be loaded from configuration correctly" in {
    val config = Configuration(
      "service.host" -> "localhost",
      "service.port" -> "8080",
      "service.protocol" -> "http"
    )

    val service = config.get[Service]("service")
    service shouldEqual Service("localhost", "8080", "http")
  }

  "should convert to string correctly" in {
    val service = Service("localhost", "8080", "http")
    val expectedUrl = "http://localhost:8080"

    service.toString shouldEqual expectedUrl
  }

  "convertToString" should {
    "return the correct output" in new Setup {

      import Service.convertToString

      convertToString(serviceOb) shouldBe "http://localhost:8080"
    }
  }

  trait Setup {
    val host = "localhost"
    val port = "8080"
    val protocol = "http"

    val serviceOb: Service = Service(host, port, protocol)
  }

}

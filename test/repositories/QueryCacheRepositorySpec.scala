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

package repositories

import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import utils.SpecBase
import utils.MustMatchers

class QueryCacheRepositorySpec extends SpecBase with MustMatchers with BeforeAndAfterEach {

  "get" should {
    "return none if no session data stored by a given id" in new Setup {
      running(app) {
        val result = await(repository.getQuery("someSessionId"))
        result mustBe None
      }
    }

    "return query if the sessionId is stored and there is query stored in the session" in new Setup {
      running(app) {
        val repository = app.injector.instanceOf[QueryCacheRepository]
        await(repository.clearAndInsertQuery("someSessionId", testQuery))
        val result     = await(repository.getQuery("someSessionId"))
        result.value mustBe testQuery
      }
    }
  }

  "clearAndInsert" should {
    "remove the existing data associated with the sessionId and populate the query" in new Setup {
      val dummyQuery: String = "dummyQuery"

      running(app) {
        val preInsertResult = await(repository.getQuery("someSessionId"))
        preInsertResult mustBe None
        await(repository.clearAndInsertQuery("someSessionId", dummyQuery))
        await(repository.clearAndInsertQuery("someSessionId", testQuery))

        val result = await(repository.getQuery("someSessionId"))

        result mustBe Some(testQuery)
      }
    }
  }

  "remove" should {
    "return true if the remove was successful" in new Setup {
      running(app) {
        await(repository.clearAndInsertQuery("someSessionId", testQuery))
        await(repository.getQuery("someSessionId")) mustBe Some(testQuery)
        await(repository.removeQuery("someSessionId"))
        await(repository.getQuery("someSessionId")) mustBe None
      }
    }
  }

  trait Setup {
    val app: Application                 = new GuiceApplicationBuilder().build()
    val repository: QueryCacheRepository = app.injector.instanceOf[QueryCacheRepository]
    val testQuery: String                = "someQuery"
  }

  override def afterEach(): Unit = {
    val app: Application                 = new GuiceApplicationBuilder().build()
    val repository: QueryCacheRepository = app.injector.instanceOf[QueryCacheRepository]
    running(app) {
      await(repository.removeQuery("someSessionId"))
    }
  }
}

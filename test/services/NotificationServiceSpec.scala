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

package services

import domain.FileRole.{C79Certificate, DutyDefermentStatement, PostponedVATStatement, SecurityStatement, StandingAuthority}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationServiceSpec extends MockAuditingService with FutureAwaits with DefaultAwaitTimeout with ScalaFutures with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val eori = "GB123456"
  val fileSize = 999L
  val mockApiService: ApiService = mock[ApiService]

  override def beforeEach(): Unit = {
    reset(mockApiService)
  }

  "Notification service" should {

    "indicate that notifications are available" when {
      val collectionOfDocumentAttributes = List(
        DocumentAttributes(eori, C79Certificate, "new file", fileSize, Map.empty),
        DocumentAttributes(eori, PostponedVATStatement, "new file", fileSize, Map.empty),
        DocumentAttributes(eori, SecurityStatement, "new file", fileSize, Map.empty),
        DocumentAttributes(eori, DutyDefermentStatement, "new file", fileSize, Map.empty),
        DocumentAttributes(eori, C79Certificate, "new file", fileSize, Map("statementRequestID" -> "3jh9f9b9-f9b9-9f9c-999a-36701e99d9")),
        DocumentAttributes(eori, DutyDefermentStatement, "new file", fileSize, Map("statementRequestID" -> "3jh9f9b9-f9b9-9f9c-999a-36701e99d9")),
        DocumentAttributes(eori, SecurityStatement, "new file", fileSize, Map("statementRequestID" -> "3jh9f9b9-f9b9-9f9c-999a-37701e99d9")),
        DocumentAttributes(eori, StandingAuthority, "new file", fileSize, Map.empty)
      )

      "the given document type is present" in {
        when(mockApiService.getEnabledNotifications(eori)).thenReturn(Future.successful(collectionOfDocumentAttributes))
        val notificationService = new NotificationService(mockApiService)

        val expectedNotification = List(
          Notification(C79Certificate, isRequested = false),
          Notification(PostponedVATStatement, isRequested = false),
          Notification(SecurityStatement, isRequested = false),
          Notification(DutyDefermentStatement, isRequested = false),
          Notification(C79Certificate, isRequested = true),
          Notification(DutyDefermentStatement, isRequested = true),
          Notification(SecurityStatement, isRequested = true),
          Notification(StandingAuthority, isRequested = false)
        )

        val actualNotification = await(notificationService.fetchNotifications(eori))

        actualNotification must be(expectedNotification)
        verify(mockApiService).getEnabledNotifications(eqTo(eori))(eqTo(hc))
      }
    }
  }
}

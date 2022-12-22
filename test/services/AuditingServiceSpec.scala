/*
 * Copyright 2022 HM Revenue & Customs
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

import config.AppConfig
import domain.{AuditEori, AuditModel, EoriHistory, SignedInUser}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.captor.{ArgCaptor, Captor}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector._
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.SpecBase
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditingServiceSpec extends SpecBase {

  "AuditingService" should {

    "create the correct audit event for view account" in new Setup {
      val validFrom: LocalDate = LocalDate.now().minusDays(30)
      val validTo: LocalDate = LocalDate.now().plusDays(30)

      val eoriHistory = Seq(
        EoriHistory("testEori1", validFrom = Some(validFrom), validUntil = Some(validTo)),
        EoriHistory("testEori2", validFrom = Some(validFrom), validUntil = Some(validTo))
      )
      val user = SignedInUser("testEori3", eoriHistory)

      val expectedAuditEvent = Json.arr(
        Json.obj(
          "eori" -> "testEori3",
          "isHistoric" -> false
        ),
        Json.obj(
          "eori" -> "testEori1",
          "isHistoric" -> true
        ),
        Json.obj(
          "eori" -> "testEori2",
          "isHistoric" -> true
        )
      )

      await(auditingService.viewAccount(user))

      val dataEventCaptor: Captor[ExtendedDataEvent] = ArgCaptor[ExtendedDataEvent]
      verify(mockAuditConnector).sendExtendedEvent(dataEventCaptor.capture)(any, any)
      val dataEvent: ExtendedDataEvent = dataEventCaptor.value

      dataEvent.auditSource shouldBe expectedAuditSource
      dataEvent.auditType shouldBe "ViewAccount"
      dataEvent.detail shouldBe expectedAuditEvent
    }

    "create the correct data event for a user requesting duty deferment statements" in new Setup {
      val model = AuditModel(AUDIT_TYPE, AUDIT_DUTY_DEFERMENT_TRANSACTION, Json.toJson(AuditEori(eori, false)))
      await(auditingService.audit(model))

      val dataEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(mockAuditConnector).sendExtendedEvent(dataEventCaptor.capture)(any, any)
      val dataEvent = dataEventCaptor.value

      dataEvent.auditSource should be(expectedAuditSource)
      dataEvent.auditType should be(AUDIT_TYPE)
      dataEvent.detail.toString() should include(eori)
      dataEvent.tags.toString() should include(AUDIT_DUTY_DEFERMENT_TRANSACTION)
    }

    "create the correct data event for a user requesting VAT certificates" in new Setup {
      val model = AuditModel(AUDIT_VAT_CERTIFICATES, AUDIT_VAT_CERTIFICATES_TRANSACTION, Json.toJson(AuditEori(eori, true)))
      await(auditingService.audit(model))

      val dataEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(mockAuditConnector).sendExtendedEvent(dataEventCaptor.capture)(any, any)
      val dataEvent = dataEventCaptor.value

      dataEvent.auditSource should be(expectedAuditSource)
      dataEvent.auditType should be(AUDIT_VAT_CERTIFICATES)
      dataEvent.detail.toString() should include(eori)
      dataEvent.tags.toString() should include(AUDIT_VAT_CERTIFICATES_TRANSACTION)
    }

    "create the correct data event for a user requesting postponed VAT certificates" in new Setup {
      val model = AuditModel(AUDIT_POSTPONED_VAT_STATEMENTS, AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(eori, false)))
      await(auditingService.audit(model))

      val dataEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(mockAuditConnector).sendExtendedEvent(dataEventCaptor.capture)(any, any)
      val dataEvent = dataEventCaptor.value

      dataEvent.auditSource should be(expectedAuditSource)
      dataEvent.auditType should be(AUDIT_POSTPONED_VAT_STATEMENTS)
      dataEvent.detail.toString() should include(eori)
      dataEvent.tags.toString() should include(AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION)
    }

    "create the correct data event for a user requesting security statements" in new Setup {
      val model = AuditModel(AUDIT_SECURITY_STATEMENTS, AUDIT_SECURITY_STATEMENTS_TRANSACTION, Json.toJson(AuditEori(eori, false)))
      await(auditingService.audit(model))

      val dataEventCaptor = ArgCaptor[ExtendedDataEvent]
      verify(mockAuditConnector).sendExtendedEvent(dataEventCaptor.capture)(any, any)
      val dataEvent = dataEventCaptor.value

      dataEvent.auditSource should be(expectedAuditSource)
      dataEvent.auditType should be(AUDIT_SECURITY_STATEMENTS)
      dataEvent.detail.toString() should include(eori)
      dataEvent.tags.toString() should include(AUDIT_SECURITY_STATEMENTS_TRANSACTION)
    }
  }

  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val expectedAuditSource = "customs-financials-frontend"
    val eori = "EORI"
    val AUDIT_DUTY_DEFERMENT_TRANSACTION = "DUTYDEFERMENTSTATEMENTS"
    val AUDIT_VAT_CERTIFICATES_TRANSACTION = "Display VAT certificates"
    val AUDIT_POSTPONED_VAT_STATEMENTS_TRANSACTION = "Display postponed VAT statements"
    val AUDIT_SECURITY_STATEMENTS_TRANSACTION = "Display security statements"
    val AUDIT_TYPE = "SDESCALL"
    val AUDIT_VAT_CERTIFICATES = "DisplayVATCertificates"
    val AUDIT_SECURITY_STATEMENTS = "DisplaySecurityStatements"
    val AUDIT_POSTPONED_VAT_STATEMENTS = "DisplayPostponedVATStatements"
    val AUDIT_AUTHORISED_TRANSACTION = "View account"
    val AUDIT_EORI = "EORI"
    val AUDIT_HISTORIC_EORIS = "HISTORIC_EORI"


    val mockConfig = mock[AppConfig]
    when(mockConfig.appName).thenReturn("customs-financials-frontend")

    val mockAuditConnector = mock[AuditConnector]
    when(mockAuditConnector.sendExtendedEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))

    val auditingService = new AuditingService(mockConfig, mockAuditConnector)
  }

}
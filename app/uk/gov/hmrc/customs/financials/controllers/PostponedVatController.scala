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

package uk.gov.hmrc.customs.financials.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.customs.financials.actionbuilders.{PvatIdentifierAction, SessionIdAction}
import uk.gov.hmrc.customs.financials.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.customs.financials.domain.DutyPaymentMethod.CHIEF
import uk.gov.hmrc.customs.financials.domain.FileRole.PostponedVATStatement
import uk.gov.hmrc.customs.financials.domain.{FileFormat, PostponedVatCertificateFile}
import uk.gov.hmrc.customs.financials.services._
import uk.gov.hmrc.customs.financials.viewmodels.PostponedVatViewModel
import uk.gov.hmrc.customs.financials.views.html.postponed_import_vat
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PostponedVatController @Inject()
(val authenticate: PvatIdentifierAction,
 val resolveSessionId: SessionIdAction,
 val documentService: DocumentService,
 implicit val dateTimeService: DateTimeService,
 postponedImportVatView: postponed_import_vat,
 apiService: ApiService,
 implicit val mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig, val errorHandler: ErrorHandler, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  val log: LoggerLike = Logger(this.getClass)

  def show(location: Option[String]): Action[AnyContent] = (authenticate andThen resolveSessionId) async { implicit req =>

    val currentEori = req.user.eori
    val filteredHistoricEoris = req.user.allEoriHistory.filterNot(_.eori == currentEori)
    apiService.deleteNotification(currentEori, PostponedVATStatement)

    def filterOnlyPvatFiles(files: Seq[PostponedVatCertificateFile]): Seq[PostponedVatCertificateFile] = {
      files.filter { p => FileFormat.PvatFileFormats.contains(p.metadata.fileFormat) }
    }

    def eventualPvatStatements: Future[Seq[PostponedVatCertificateFile]] = {
      documentService.getPostponedVatStatements(currentEori).map { a =>
        log.info("POSTPONEDVATSTATEMENTS" + a.toString())
        filterOnlyPvatFiles(a)
      }
    }

    def eventualHistoricPvatStatements: Future[Seq[PostponedVatCertificateFile]] = {
      Future.sequence(filteredHistoricEoris.map { eoriHistory =>
        documentService.getPostponedVatStatements(eoriHistory.eori).map { response =>
          filterOnlyPvatFiles(response)
        }
      }).map(_.flatten)
    }

    for {
      pVatStatements <- eventualPvatStatements
      historicPVatStatements <- eventualHistoricPvatStatements
    } yield {
      val allPVatStatements = pVatStatements ++ historicPVatStatements
      val allPvatStatementsCount = allPVatStatements.size
      val cdsCount: Int = allPVatStatements.count(_.metadata.source != CHIEF)
      val cdsOnly: Boolean = cdsCount == allPvatStatementsCount
      log.info(s"postponed vat statements displayed TOTAL: ${allPvatStatementsCount} CDS: $cdsCount CHIEF: ${allPvatStatementsCount - cdsCount}")
      Ok(postponedImportVatView(currentEori, PostponedVatViewModel(allPVatStatements), cdsOnly, location))
    }
  }

}



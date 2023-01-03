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

package domain

import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.PathBindable
import play.api.{Logger, LoggerLike}
import views.helpers.Formatters

import java.time.LocalDate
import scala.collection.immutable.SortedSet

sealed abstract class FileFormat(val name: String) extends Ordered[FileFormat] {
  val order: Int

  def compare(that: FileFormat): Int = order.compare(that.order)

  override def toString: String = name
}

object FileFormat {

  // scalastyle:off magic.number
  case object Pdf extends FileFormat("PDF") {
    val order = 1
  }

  case object Csv extends FileFormat("CSV") {
    val order = 2
  }

  case object UnknownFileFormat extends FileFormat("UNKNOWN FILE FORMAT") {
    val order = 99
  }
  // scalastyle:on magic.number

  val log: LoggerLike = Logger(this.getClass)

  val SdesFileFormats: SortedSet[FileFormat] = SortedSet(Pdf, Csv)
  val PvatFileFormats: SortedSet[FileFormat] = SortedSet(Pdf)
  val authorityFileFormats: SortedSet[FileFormat] = SortedSet(Csv)

  def filterFileFormats[T <: SdesFile](allowedFileFormats: SortedSet[FileFormat])(files: Seq[T]): Seq[T] = files.filter(file => allowedFileFormats(file.metadata.fileFormat))

  def apply(name: String): FileFormat = name.toUpperCase match {
    case Pdf.name => Pdf
    case Csv.name => Csv
    case _ =>
      log.warn(s"Unknown file format: $name")
      UnknownFileFormat
  }

  def unapply(arg: FileFormat): Option[String] = Some(arg.name)

  implicit val fileFormatFormat = new Format[FileFormat] {
    def reads(json: JsValue) = JsSuccess(apply(json.as[String]))

    def writes(obj: FileFormat) = JsString(obj.name)
  }
}

sealed abstract class DDStatementType(val name: String) extends Ordered[DDStatementType] {
  val order: Int

  def compare(that: DDStatementType): Int = order.compare(that.order)
}

object DDStatementType {
  val log: LoggerLike = Logger(this.getClass)

  case object Excise extends DDStatementType("Excise") {
    val order = 1
  }

  case object Supplementary extends DDStatementType(name = "Supplementary") {
    val order = 2
  }

  case object Weekly extends DDStatementType("Weekly") {
    val order = 3
  }

  case object UnknownStatementType extends DDStatementType("UNKNOWN STATEMENT TYPE") {
    val order = 4
  }

  def apply(name: String): DDStatementType = name match {
    case Weekly.name => Weekly
    case Supplementary.name => Supplementary
    case Excise.name => Excise
    case _ =>
      log.warn(s"Unknown duty deferment statement type: $name")
      UnknownStatementType
  }

  def unapply(arg: DDStatementType): Option[String] = Some(arg.name)
}

sealed abstract class FileRole(val name: String, val featureName: String, val transactionName: String, val messageKey: String)

object FileRole {

  case object DutyDefermentStatement extends FileRole("DutyDefermentStatement", "duty-deferment", "Download duty deferment statement", "duty-deferment")

  case object C79Certificate extends FileRole("C79Certificate", "import-vat", "Download import VAT statement", "c79")

  case object PostponedVATStatement extends FileRole("PostponedVATStatement", "postponed-vat", "Download postponed VAT statement", "postponed-vat")

  case object PostponedVATAmendedStatement extends FileRole("PostponedVATAmendedStatement", "postponed-vat", "Download postponed VAT amend statement", "postponed-vat")

  case object SecurityStatement extends FileRole("SecurityStatement", "adjustments", "Download adjustments statement", "adjustments")

  case object StandingAuthority extends FileRole("StandingAuthority", "authorities", "Display standing authorities csv", "authorities")


  val log: LoggerLike = Logger(this.getClass)

  def apply(name: String): FileRole = name match {
    case "DutyDefermentStatement" => DutyDefermentStatement
    case "C79Certificate" => C79Certificate
    case "PostponedVATStatement" => PostponedVATStatement
    case "PostponedVATAmendedStatement" => PostponedVATAmendedStatement
    case "SecurityStatement" => SecurityStatement
    case "StandingAuthority" => StandingAuthority
    case _ => throw new Exception(s"Unknown file role: $name")
  }

  def unapply(fileRole: FileRole): Option[String] = Some(fileRole.name)

  implicit val fileRoleFormat = new Format[FileRole] {
    def reads(json: JsValue) = JsSuccess(apply(json.as[String]))

    def writes(obj: FileRole) = JsString(obj.name)
  }

  implicit val pathBinder: PathBindable[FileRole] = new PathBindable[FileRole] {
    override def bind(key: String, value: String): Either[String, FileRole] = {
      value match {
        case "import-vat" => Right(C79Certificate)
        case "postponed-vat" => Right(PostponedVATStatement)
        case "duty-deferment" => Right(DutyDefermentStatement)
        case "adjustments" => Right(SecurityStatement)
        case "authorities" => Right(StandingAuthority)
        case fileRole => Left(s"unknown file role: ${fileRole}")
      }
    }

    override def unbind(key: String, fileRole: FileRole): String = {
      fileRole match {
        case C79Certificate => "import-vat"
        case PostponedVATStatement => "postponed-vat"
        case DutyDefermentStatement => "duty-deferment"
        case SecurityStatement => "adjustments"
        case StandingAuthority => "authorities"
        case _ => "unsupported-file-role"
      }
    }
  }

}

trait SdesFileMetadata {
  this: Product =>

  def fileFormat: FileFormat

  def fileRole: FileRole

  def periodStartYear: Int

  def periodStartMonth: Int

  def toMap[T <: SdesFileMetadata with Product]: Map[String, String] = {
    val fieldNames: Seq[String] = getClass.getDeclaredFields.map(_.getName)
    val fieldValues: Seq[String] = productIterator.to.map(_.toString)
    fieldNames.zip(fieldValues).toMap
  }
}

trait SdesFile {
  def metadata: SdesFileMetadata

  def downloadURL: String

  val fileFormat: FileFormat = metadata.fileFormat
  val monthAndYear: LocalDate = LocalDate.of(metadata.periodStartYear, metadata.periodStartMonth, 1)

  def auditModelFor(eori: EORI): AuditModel = {
    val downloadStatementAuditData = DownloadStatementAuditData.apply(metadata, eori)
    val data = downloadStatementAuditData.auditData
    val auditModel = AuditModel("DisplayStandingAuthoritiesCSV", metadata.fileRole.transactionName, Json.toJson(data))
    auditModel
  }
}

case class DutyDefermentStatementFile(filename: String,
                                      downloadURL: String,
                                      size: Long,
                                      metadata: DutyDefermentStatementFileMetadata)
  extends Ordered[DutyDefermentStatementFile] with SdesFile {

  def compare(that: DutyDefermentStatementFile): Int = fileFormat.compare(that.fileFormat)

  val startDate: LocalDate = LocalDate.of(metadata.periodStartYear, metadata.periodStartMonth, metadata.periodStartDay)
  val endDate: LocalDate = LocalDate.of(metadata.periodEndYear, metadata.periodEndMonth, metadata.periodEndDay)
}

case class DutyDefermentStatementFileMetadata(periodStartYear: Int,
                                              periodStartMonth: Int,
                                              periodStartDay: Int,
                                              periodEndYear: Int,
                                              periodEndMonth: Int,
                                              periodEndDay: Int,
                                              fileFormat: FileFormat,
                                              fileRole: FileRole,
                                              defermentStatementType: DDStatementType,
                                              dutyOverLimit: Option[Boolean],
                                              dutyPaymentType: Option[String],
                                              dan: String,
                                              statementRequestId: Option[String]) extends SdesFileMetadata

case class SecurityStatementFile(filename: String,
                                 downloadURL: String,
                                 size: Long,
                                 metadata: SecurityStatementFileMetadata
                                ) extends Ordered[SecurityStatementFile] with SdesFile {

  val startDate: LocalDate = LocalDate.of(metadata.periodStartYear, metadata.periodStartMonth, metadata.periodStartDay)
  val endDate: LocalDate = LocalDate.of(metadata.periodEndYear, metadata.periodEndMonth, metadata.periodEndDay)
  val formattedSize: String = Formatters.fileSize(metadata.fileSize)

  def compare(that: SecurityStatementFile): Int = startDate.compareTo(that.startDate)
}

case class SecurityStatementFileMetadata(periodStartYear: Int,
                                         periodStartMonth: Int,
                                         periodStartDay: Int,
                                         periodEndYear: Int,
                                         periodEndMonth: Int,
                                         periodEndDay: Int,
                                         fileFormat: FileFormat,
                                         fileRole: FileRole,
                                         eoriNumber: String,
                                         fileSize: Long,
                                         checksum: String,
                                         statementRequestId: Option[String]) extends SdesFileMetadata


case class StandingAuthorityMetadata(periodStartYear: Int,
                                     periodStartMonth: Int,
                                     periodStartDay: Int,
                                     fileFormat: FileFormat,
                                     fileRole: FileRole) extends SdesFileMetadata


case class StandingAuthorityFile(filename: String,
                                 downloadURL: String,
                                 size: Long,
                                 metadata: StandingAuthorityMetadata,
                                 eori: String) extends Ordered[StandingAuthorityFile] with SdesFile {

  val startDate: LocalDate = LocalDate.of(metadata.periodStartYear, metadata.periodStartMonth, metadata.periodStartDay)

  def compare(that: StandingAuthorityFile): Int = startDate.compareTo(that.startDate)
}

case class VatCertificateFile(filename: String, downloadURL: String, size: Long, metadata: VatCertificateFileMetadata, eori: String)(implicit messages: Messages)
  extends Ordered[VatCertificateFile] with SdesFile {

  val formattedSize: String = Formatters.fileSize(size)
  val formattedMonth: String = Formatters.dateAsMonth(monthAndYear)

  def compare(that: VatCertificateFile): Int = that.metadata.fileFormat.compare(metadata.fileFormat)
}

case class SdesFileWithId[A <: SdesFile](sdesFile: A, id: String)

case class PostponedVatStatementFile(filename: String, downloadURL: String, size: Long, metadata: PostponedVatStatementFileMetadata, eori: String)
  extends Ordered[PostponedVatStatementFile] with SdesFile {

  def compare(that: PostponedVatStatementFile): Int = that.metadata.fileFormat.compare(metadata.fileFormat)
}

case class VatCertificateFileMetadata(periodStartYear: Int,
                                      periodStartMonth: Int,
                                      fileFormat: FileFormat,
                                      fileRole: FileRole,
                                      statementRequestId: Option[String]) extends SdesFileMetadata {
}

case class PostponedVatStatementFileMetadata(periodStartYear: Int,
                                               periodStartMonth: Int,
                                               fileFormat: FileFormat,
                                               fileRole: FileRole,
                                               source: String,
                                               statementRequestId: Option[String]) extends SdesFileMetadata {
}

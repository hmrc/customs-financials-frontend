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

import domain.StandingAuthorityFile

import scala.collection.immutable.Seq

object Utils {
  val emptyString = ""
  val gbEORIPrefix = "GB"
  val gbnEORIPrefix = "GBN"
  val xiEORIPrefix = "XI"
  val danRegex = "^[0-9]{7}"
  val canRegex = "^[0-9]{11}"
  val ganRegex = "^[a-zA-Z0-9]{8,10}"
  val xiCsvFileNameRegEx = "SA_[\\w]+_XI_csv.csv$"
  val xiEoriRegex = "XI\\d{12}"

  /**
   * Returns true if the input is a valid Account number otherwise false
   * Returns false if the input is a EORI
   *
   * @param inputStr SearchQuery
   * @return Boolean
   */
  def isSearchQueryAnAccountNumber(inputStr: String): Boolean =
    !inputStr.startsWith(gbEORIPrefix) &&
      !inputStr.startsWith(gbnEORIPrefix) &&
      !inputStr.startsWith(xiEORIPrefix) && (
      inputStr.matches(danRegex) || inputStr.matches(canRegex) || inputStr.matches(ganRegex))

  /**
   * Partitions the Seq of StandingAuthorityFile into Seq of StandingAuthorityFile
   * for GB and XI authority by provided file name pattern
   *
   * @param csvFiles Seq[StandingAuthorityFile]
   * @return CsvFiles
   */
  def partitionCsvFilesByFileNamePattern(csvFiles: Seq[StandingAuthorityFile],
                                         fileNamePattern: String = xiCsvFileNameRegEx): CsvFiles = {
    val partitionedList = csvFiles.partition(stanAuth => stanAuth.filename.matches(fileNamePattern))
    CsvFiles(partitionedList._2, partitionedList._1)
  }

  /**
   * Returns true if input string is XI EORI otherwise returns false
   * Example: isXIEori("XI123456789012") = true
   * Example: isXIEori("xi123456789012") = false
   */
  def isXIEori(str: String): Boolean = str.trim.matches(xiEoriRegex)

  case class CsvFiles(gbCsvFiles: Seq[StandingAuthorityFile], xiCsvFiles: Seq[StandingAuthorityFile])
}

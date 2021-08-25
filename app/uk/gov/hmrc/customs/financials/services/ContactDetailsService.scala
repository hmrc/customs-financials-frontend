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

package uk.gov.hmrc.customs.financials.services

import java.net.URLEncoder.encode

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.crypto.{CryptoGCMWithKeysFromConfig, PlainText}
import uk.gov.hmrc.customs.financials.config.AppConfig

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactDetailsService @Inject() (implicit appConfig: AppConfig,  executionContext: ExecutionContext){

  val log: LoggerLike = Logger(this.getClass)

  val config = ConfigFactory.parseMap(
    Map(
      appConfig.contactDetailsCryptoBaseConfigKey + ".key" -> appConfig.contactDetailsCryptoEncryptionKey
    ).asJava
  )

  def getEncyptedDanWithStatus(dan: String, statusId: Int): Future[String] = {
    val danWithStatus = s"$dan|$statusId"
    val crypto = new CryptoGCMWithKeysFromConfig(appConfig.contactDetailsCryptoBaseConfigKey, config)
    val encryptedDanWithStatus = encode(crypto.encrypt(PlainText(danWithStatus)).value, "UTF8")
    log.info(s"encrypting danWithStatus: $danWithStatus encryptedDanWithStatus: $encryptedDanWithStatus")
    Future(appConfig.contactDetailsUri + encryptedDanWithStatus)
  }
}

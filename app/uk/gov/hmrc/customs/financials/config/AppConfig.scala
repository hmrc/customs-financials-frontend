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

package uk.gov.hmrc.customs.financials.config

import play.api.i18n.Lang
import play.api.{Configuration, Environment}
import uk.gov.hmrc.customs.financials.domain.FileRole
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(val config: Configuration, val environment: Environment, servicesConfig: ServicesConfig) {

  private def loadConfig(key: String) = config.get[String](key)

  lazy val appName = loadConfig("appName")
  lazy val registerCdsUrl = loadConfig("microservice.services.customs-financials-frontend.cdsRegisterUrl")
  lazy val subscribeCdsUrl: String = config.get[String]("external-urls.cdsSubscribeUrl")
  lazy val applicationStatusCdsUrl: String =config.get[String]("external-urls.applicationStatusUrl")
  lazy val feedbackService = config.getOptional[String]("microservice.services.feedback.url").getOrElse("/feedback") + config.getOptional[String]("feedback.source").getOrElse("/CDS-FIN")
  lazy val accessibilityLinkUrl = config.get[String]("external-urls.accessibility-statement")
  lazy val sessionTimeout = loadConfig("timeout-time")
  lazy val cashAccountTopUpGuidanceUrl = loadConfig("external-urls.cashAccountTopUpGuidanceUrl")
  lazy val cashAccountUrl = loadConfig("microservice.services.customs-cash-account-frontend.url")
  lazy val manageAuthoritiesFrontendUrl = config.get[String]("microservice.services.customs-manage-authorities-frontend.url")
  lazy val guaranteeAccountUrl = config.get[String]("microservice.services.customs-guarantee-account-frontend.url")
  lazy val dutyDefermentSchemeContactLink = loadConfig("external-urls.dutyDefermentSchemeContactLink")
  lazy val dutyDefermentTopUpLink = loadConfig("external-urls.dutyDefermentTopUpLink")

  lazy val sddsUri: String = servicesConfig.baseUrl("sdds") + config.getOptional[String]("microservice.services.sdds.context").getOrElse("/direct-debit-backend") + "/cds-homepage/cds/journey/start"
  lazy val contactDetailsUri = config.get[String]("microservice.services.customs-financials-account-contact-frontend.url") + "/duty-deferment/"
  lazy val contactDetailsCryptoBaseConfigKey = config.getOptional[String]("microservice.services.customs-financials-account-contact-frontend.crypto.baseConfigKey").getOrElse("cookie.encryption")
  lazy val contactDetailsCryptoEncryptionKey = config.getOptional[String]("microservice.services.customs-financials-account-contact-frontend.crypto.encryptionKey").getOrElse("gvBoGdgzqG1AarzF1LY0zQ==")

  lazy val loginUrl: String = config.get[String]("external-urls.login")
  lazy val loginContinueUrl: String = config.get[String]("external-urls.loginContinue")
  lazy val pvatLoginContinueUrl: String = config.get[String]("external-urls.pvatLoginContinue")
  lazy val signOutUrl: String = config.get[String]("external-urls.signOut")

  lazy val languageTranslationEnabled: Boolean = config.get[Boolean]("features.welsh-translation")

  lazy val xClientIdHeader = loadConfig("microservice.services.sdes.x-client-id")


  lazy val customsFinancialsApi = servicesConfig.baseUrl("customs-financials-api") + config.getOptional[String]("microservice.services.customs-financials-api.context").getOrElse("/customs-financials-api")
  lazy val customsDataStore = servicesConfig.baseUrl("customs-data-store") + config.getOptional[String]("microservice.services.customs-data-store.context").getOrElse("/customs-data-store")
  lazy val customsFinancialsSessionCacheUrl = servicesConfig.baseUrl("customs-financials-session-cache") + config.getOptional[String]("microservice.services.customs-financials-session-cache.context").getOrElse("/customs/session-cache")

  lazy val sdesApi = servicesConfig.baseUrl("sdes") + config.getOptional[String]("microservice.services.sdes.context").getOrElse("/")
  lazy val customsFinancialFrontend = config.getOptional[String]("microservice.services.customs-financials-frontend.url").getOrElse("/customs-financials-frontend")

  lazy val customsEmailFrontend = config.getOptional[String]("microservice.services.customs-email-frontend.url").getOrElse("/customs-email-frontend")

  lazy val numberOfItemsPerPage = config.getOptional[Int]("microservice.services.customs-financials-frontend.numberOfItemsPerPage").getOrElse(1)
  lazy val sessionCacheExpiryInSeconds = config.getOptional[Int]("microservice.services.customs-financials-frontend.sessionCache.expirySeconds").getOrElse(0)

  lazy val serviceNameSdes = loadConfig("microservice.services.sdes.circuit-breaker.serviceName")
  lazy val numberOfCallsToSwitchCircuitBreakerSdes = loadConfig("microservice.services.sdes.circuit-breaker.numberOfCallsToTriggerStateChange").toInt
  lazy val unavailablePeriodDurationSdes = loadConfig("microservice.services.sdes.circuit-breaker.unavailablePeriodDuration").toInt
  lazy val unstablePeriodDurationSdes = loadConfig("microservice.services.sdes.circuit-breaker.unstablePeriodDuration").toInt

  private lazy val historicRequest = config.get[String]("external-urls.historicRequest")
  private lazy val requestedStatements = config.get[String]("external-urls.requestedStatements")

  def historicRequestUrl(fileRole: FileRole, linkId: String): String = {
    fileRole match {
      case FileRole.DutyDefermentStatement => historicRequest + s"duty-deferment/$linkId"
      case _ => ""
    }
  }

  def historicRequestUrl(fileRole: FileRole): String = {
    fileRole match {
      case FileRole.C79Certificate => historicRequest + fileRole.featureName
      case FileRole.SecurityStatement => historicRequest + "adjustments"
      case _ => ""
    }
  }

  def requestedStatements(fileRole: FileRole, linkId: String): String = {
    fileRole match {
      case FileRole.DutyDefermentStatement => requestedStatements + s"duty-deferment/$linkId"
      case _ => ""
    }
  }

  def requestedStatements(fileRole: FileRole): String = {
    fileRole match {
      case FileRole.C79Certificate => requestedStatements + fileRole.featureName
      case FileRole.SecurityStatement => requestedStatements + "adjustments"
      case _ => ""
    }
  }

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  lazy val helpMakeGovUkBetterUrl = loadConfig("external-urls.helpMakeGovUkBetterUrl")
  lazy val govukHome: String = config.get[String]("external-urls.govUkHome")
  lazy val reportAProblem = config.getOptional[Boolean]("features.report-a-problem").getOrElse(false)
  lazy val fixedDateTime = config.get[Boolean]("features.fixed-system-time")

  //for secure message banner
  private val customsSecureMessagingBaseUrl: String = config.get[Service]("microservice.services.customs-financials-secure-messaging-frontend").baseUrl
  private val customsSecureMessagingContext: String = config.get[String]("microservice.services.customs-financials-secure-messaging-frontend.context")
  lazy val customsSecureMessagingBannerEndpoint: String = {
    customsSecureMessagingBaseUrl +
      customsSecureMessagingContext +
      config.get[String]("microservice.services.customs-financials-secure-messaging-frontend.banner-endpoint")
  }
}

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

import domain.FileRole
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName: String = config.get[String]("appName")

  lazy val emailFrontendService: String =
    s"${servicesConfig.baseUrl("customs-email-frontend")}${config.get[String]("microservice.services.customs-email-frontend.context")}"

  lazy val timeout: Int   = config.get[Int]("timeout.timeout")
  lazy val countdown: Int = config.get[Int]("timeout.countdown")

  lazy val xClientIdHeader: String                        = config.get[String]("microservice.services.sdes.x-client-id")
  lazy val fixedDateTime: Boolean                         = config.get[Boolean]("features.fixed-system-time")
  lazy val xiEoriEnabled: Boolean                         = config.get[Boolean]("features.xi-eori-enabaled")
  lazy val isCashAccountV2FeatureFlagEnabled: Boolean     = config.get[Boolean]("features.cash-account-v2-enabled")
  lazy val isHomePageLinksEnabled: Boolean                = config.get[Boolean]("features.home-page-links-enabled")
  lazy val isAuthoritiesNotificationPanelEnabled: Boolean =
    config.get[Boolean]("features.authorities-notification-panel-enabled")

  lazy val isEUEoriEnabled: Boolean = config.get[Boolean]("features.eu-eori-enabled")

  lazy val subscribeCdsUrl: String             = config.get[String]("external-urls.cdsSubscribeUrl")
  lazy val manageTeamMembersUrl: String        = config.get[String]("external-urls.manageTeamMembers")
  lazy val onlineServicesHelpUrl: String       = config.get[String]("external-urls.onlineServicesHelp")
  lazy val reportChangeCdsUrl: String          = config.get[String]("external-urls.reportChangeUrl")
  lazy val accessibilityLinkUrl: String        = config.get[String]("external-urls.accessibility-statement")
  lazy val cashAccountTopUpGuidanceUrl: String = config.get[String]("external-urls.cashAccountTopUpGuidanceUrl")
  lazy val cashAccountWithdrawUrl: String      = config.get[String]("external-urls.cashAccountWithdrawUrl")
  lazy val eoriChangeOrCancelURL: String       = config.get[String]("external-urls.eoriChangeOrCancelURL")

  lazy val dutyDefermentTopUpLink: String = config.get[String]("external-urls.dutyDefermentTopUpLink")
  lazy val loginUrl: String               = config.get[String]("external-urls.login")
  lazy val loginContinueUrl: String       = config.get[String]("external-urls.loginContinue")
  lazy val signOutUrl: String             = config.get[String]("external-urls.signOut")
  lazy val helpMakeGovUkBetterUrl: String = config.get[String]("external-urls.helpMakeGovUkBetterUrl")

  lazy val financialsFrontendUrl: String =
    config.get[String]("microservice.services.customs-financials-frontend.url")

  lazy val cashAccountUrl: String =
    if (isCashAccountV2FeatureFlagEnabled) {
      config.get[String]("microservice.services.customs-cash-account-frontend.urlV2")
    } else {
      config.get[String]("microservice.services.customs-cash-account-frontend.url")
    }

  lazy val cashAccountTestUrl: String =
    if (isCashAccountV2FeatureFlagEnabled) {
      config.get[String]("microservice.services.customs-cash-account-frontend.testURL")
    } else {
      config.get[String]("microservice.services.customs-cash-account-frontend.testURL")
    }

  lazy val manageAuthoritiesFrontendUrl: String =
    config.get[String]("microservice.services.customs-manage-authorities-frontend.url")

  private lazy val manageAuthoritiesServiceBaseUrl    = s"${servicesConfig.baseUrl("customs-manage-authorities-frontend")}"
  private lazy val manageAuthoritiesServiceContextUrl =
    s"${config.get[String]("microservice.services.customs-manage-authorities-frontend.context")}"

  lazy val manageAuthoritiesServiceUrl = s"$manageAuthoritiesServiceBaseUrl$manageAuthoritiesServiceContextUrl"

  lazy val guaranteeAccountUrl: String =
    config.get[String]("microservice.services.customs-guarantee-account-frontend.url")

  lazy val emailFrontendUrl: String = s"$emailFrontendService/service/customs-finance"
  lazy val documentsUrl: String     =
    config.get[String]("microservice.services.customs-financials-documents-frontend.url")

  lazy val importVATAccountUrl: String    = s"$documentsUrl/import-vat"
  lazy val postponedVATAccountUrl: String = s"$documentsUrl/postponed-vat?location=CDS"
  lazy val securitiesAccountUrl: String   = s"$documentsUrl/adjustments"
  lazy val changeEmailUrl: String         = config.get[String]("external-urls.changeEmailAddressUrl")

  lazy val feedbackService: String = config.get[String]("microservice.services.feedback.url") +
    config.get[String]("microservice.services.feedback.source")

  lazy val customsFinancialsSessionCacheUrl: String = servicesConfig.baseUrl("customs-financials-session-cache") +
    config.get[String]("microservice.services.customs-financials-session-cache.context")

  def contactDetailsUrl(linkId: String): String =
    s"${config.get[String]("microservice.services.customs-duty-deferment-frontend.url")}/$linkId/contact-details"

  def accountUrl(linkId: String): String =
    s"${config.get[String]("microservice.services.customs-duty-deferment-frontend.url")}/$linkId/account"

  def directDebitUrl(linkId: String): String =
    s"${config.get[String]("microservice.services.customs-duty-deferment-frontend.url")}/$linkId/direct-debit"

  lazy val customsSecureMessagingBannerEndpoint: String =
    config.get[Service]("microservice.services.customs-financials-secure-messaging-frontend").baseUrl +
      config.get[String]("microservice.services.customs-financials-secure-messaging-frontend.context") +
      config.get[String]("microservice.services.customs-financials-secure-messaging-frontend.banner-endpoint")

  lazy val customsFinancialsApi: String = servicesConfig.baseUrl("customs-financials-api") +
    config.get[String]("microservice.services.customs-financials-api.context")

  lazy val customsDataStore: String = servicesConfig.baseUrl("customs-data-store") +
    config.get[String]("microservice.services.customs-data-store.context")

  lazy val sdesApi: String = servicesConfig.baseUrl("sdes") +
    config.get[String]("microservice.services.sdes.context")

  def filesUrl(fileRole: FileRole): String = s"$sdesApi/files-available/list/${fileRole.name}"
}

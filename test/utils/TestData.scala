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

import play.twirl.api.Html

object TestData {

  val TEST_ID   = "test_id"
  val ITEMS_20  = 20
  val ITEMS_40  = 40
  val ITEMS_45  = 45
  val ITEMS_245 = 245
  val ITEMS_250 = 250

  val PAGINATOR_ELEMENT_1 = 1
  val PAGINATOR_ELEMENT_3 = 3
  val PAGINATOR_ELEMENT_4 = 4
  val PAGINATOR_ELEMENT_5 = 5
  val PAGINATOR_ELEMENT_6 = 6

  val PAGE_0  = 0
  val PAGE_1  = 1
  val PAGE_2  = 2
  val PAGE_3  = 3
  val PAGE_4  = 4
  val PAGE_5  = 5
  val PAGE_6  = 6
  val PAGE_7  = 7
  val PAGE_8  = 8
  val PAGE_9  = 9
  val PAGE_10 = 10
  val PAGE_50 = 50

  val PAGE_NEGATIVE_50: Int = -50

  val BALANCE_10                = 10
  val BALANCE_20                = 20
  val BALANCE_50                = 50
  val BALANCE_100               = 100
  val BALANCE_150               = 150
  val BALANCE_200               = 200
  val BALANCE_299               = 299
  val BALANCE_300               = 300
  val BALANCE_499               = 499
  val BALANCE_500               = 500
  val BALANCE_876               = 876
  val BALANCE_888               = 888
  val BALANCE_987               = 987
  val BALANCE_999               = 999
  val BALANCE_1000000           = 1000000
  val BALANCE_200000            = 200000
  val BALANCE_200001            = 200001
  val BALANCE_200002            = 200002
  val NEGATIVE_BALANCE_10: Int  = -10
  val NEGATIVE_BALANCE_50: Int  = -50
  val NEGATIVE_BALANCE_100: Int = -100

  val YEAR_1972 = 1972
  val YEAR_2010 = 2010
  val YEAR_2017 = 2017
  val YEAR_2022 = 2022
  val YEAR_2023 = 2023
  val YEAR_2027 = 2027

  val MONTH_1  = 1
  val MONTH_2  = 2
  val MONTH_5  = 5
  val MONTH_6  = 6
  val MONTH_12 = 12

  val DAY_1  = 1
  val DAY_2  = 2
  val DAY_20 = 20
  val DAY_25 = 25

  val HOUR_12    = 12
  val MINUTES_30 = 30

  val FILE_SIZE_DEFAULT = 1234L
  val FILE_SIZE_42      = 42
  val FILE_SIZE_111     = 111L
  val FILE_SIZE_115     = 115L
  val FILE_SIZE_500     = 500L
  val FILE_SIZE_888     = 888L
  val FILE_SIZE_1000    = 1000L
  val FILE_SIZE_2064    = 2064L
  val FILE_SIZE_2164    = 2164L
  val FILE_SIZE_999999  = 999999L
  val FILE_SIZE_1000000 = 1000000L
  val FILE_SIZE_5430000 = 5430000L

  val LENGTH_8  = 8
  val LENGTH_11 = 11
  val LENGTH_27 = 27

  val TEST_EORI        = "GB12345678"
  val TEST_STATUS      = "pending"
  val TEST_STATUS_TEXT = "test_status"

  val TEST_DATE    = "2024-10-01"
  val TEST_ACK_REF = "test_ref"
  val REGIME_CDS   = "CDS"

  val ACC_TYPE_CDS_CASH = "Cash account"
  val ACC_NUMBER        = "1234567"

  val TEST_EMAIL     = "test@test.com"
  val TEST_KEY       = "test_key"
  val TEST_KEY_VALUE = "test_value"

  lazy val TEST_MESSAGE_BANNER: Html = Html("""<html>
      | <head></head>
      | <body>
      |  <div class="govuk-!-padding-bottom-3 govuk-!-padding-top-3 notifications-bar">
      |   <ul class="govuk-list">
      |    <li><a class="govuk-link" href="http://localhost:9876/customs/payment-records">Home</a></li>
      |    <li class="notifications-bar-ul-li">
      |      <a class="govuk-link" href="http://localhost:9842/customs/secure-messaging/inbox?return_to=test_url">
      |          Messages<span class="hmrc-notification-badge">2</span>
      |      </a>
      |   </li>
      |    <li>
      |      <a class="govuk-link" href="http://localhost:9876/customs/payment-records/your-contact-details">
      |        Your contact details
      |      </a>
      |    </li>
      |    <li>
      |      <a class="govuk-link" href="http://localhost:9000/customs/manage-authorities">
      |        Your account authorities
      |      </a>
      |    </li>
      |   </ul>
      |  </div>
      |  <hr class="govuk-section-break govuk-section-break--visible" aria-hidden="true">
      | </body>
      |</html>""".stripMargin)
}

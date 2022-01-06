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

package domain

import play.api.i18n.Messages
import views.helpers.Formatters

trait CDSAccount {
  val number: String
  val owner: String
  val status: CDSAccountStatus
  val statusId: CDSAccountStatusId
}

// scalastyle:off cyclomatic.complexity
case class DutyDefermentAccount(number: String,
                                owner: String,
                                status: CDSAccountStatus,
                                statusId: CDSAccountStatusId,
                                balances: DutyDefermentBalance,
                                viewBalanceIsGranted: Boolean,
                                isIsleOfMan: Boolean) extends Ordered[DutyDefermentAccount] with CDSAccount {
  override def compare(that: DutyDefermentAccount): Int = number.compareTo(that.number)

  def displayAccountLimit(eori: String): Boolean = owner == eori

  def displayBalances(periodAccountLimit: Option[BigDecimal],
                      periodGuaranteeLimit: Option[BigDecimal],
                      periodGuaranteeLimitRemaining: Option[BigDecimal]
                     )(implicit messages: Messages): Option[DutyDefermentDisplayBalance] = {

    (periodAccountLimit, periodGuaranteeLimit, periodGuaranteeLimitRemaining) match {
      case (Some(accountLimit), Some(guaranteeLimit), Some(availableGuaranteeBalance)) if accountLimit > 0 && guaranteeLimit > 0 => {
        Some(
          DutyDefermentDisplayBalance(
            Formatters.formatCurrencyAmount(accountLimit),
            Formatters.formatCurrencyAmount(guaranteeLimit),
            Formatters.formatCurrencyAmount(availableGuaranteeBalance))
        )
      }
      case (Some(accountLimit), Some(guaranteeLimit), Some(_)) if accountLimit > 0 && guaranteeLimit == 0 => {
        Some(
          DutyDefermentDisplayBalance(
            Formatters.formatCurrencyAmount(accountLimit),
            messages("cf.duty-deferment.account.card.not-applicable"),
            messages("cf.duty-deferment.account.card.not-applicable"))
        )
      }
      case (Some(accountLimit), Some(guaranteeLimit), Some(availableGuaranteeBalance)) if accountLimit == 0 && guaranteeLimit > 0 => {
        Some(
          DutyDefermentDisplayBalance(
            messages("cf.duty-deferment.account.card.not-applicable"),
            Formatters.formatCurrencyAmount(guaranteeLimit),
            Formatters.formatCurrencyAmount(availableGuaranteeBalance))
        )
      }
      case _ => None
    }
  }

  val isDirectDebitSetupRequired: Boolean = {
    (statusId, status, isIsleOfMan) match {
      case (DirectDebitMandateCancelled, _, false) if (status != AccountStatusPending) => true
      case _ => false
    }
  }
}

case class GeneralGuaranteeAccount(number: String,
                                   owner: String,
                                   status: CDSAccountStatus,
                                   statusId: CDSAccountStatusId,
                                   balances: Option[GeneralGuaranteeBalance]
                                  ) extends CDSAccount

case class CashAccount(number: String,
                       owner: String,
                       status: CDSAccountStatus,
                       statusId: CDSAccountStatusId,
                       balances: CDSCashBalance
                      ) extends CDSAccount

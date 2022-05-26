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

import scala.math.Numeric.BigDecimalIsFractional.zero

trait Balances

case class DutyDefermentBalance(periodGuaranteeLimit: Option[BigDecimal],
                                periodAccountLimit: Option[BigDecimal],
                                periodAvailableGuaranteeBalance: Option[BigDecimal],
                                periodAvailableAccountBalance: Option[BigDecimal]
                               ) extends Balances {
  val MAX = BigDecimal(100.00)
  val MIN = BigDecimal(0)

  val (usedFunds, usedPercentage) = (periodAccountLimit, periodAvailableAccountBalance) match {
    case (Some(limit), Some(balance)) if limit <= MIN => {
      val fundsUsed = limit - balance
      (fundsUsed, MAX)
    }
    case (Some(limit), Some(balance)) => {
      val fundsUsed = limit - balance
      val percentage = fundsUsed / limit * MAX
      (fundsUsed, percentage)
    }
    case _ => (MIN, MIN)
  }

  val hasApprovalScheme: Boolean = {
    (periodGuaranteeLimit.isDefined, periodAccountLimit.isDefined) match {
      case (true, false) => false
      case (_, _) => false
    }
  }

  val availableBalance: BigDecimal = periodAvailableAccountBalance.getOrElse(MIN)
}

case class GeneralGuaranteeBalance(GuaranteeLimit: BigDecimal,
                                   AvailableGuaranteeBalance: BigDecimal) extends Balances {

  val usedFunds: BigDecimal = GuaranteeLimit - AvailableGuaranteeBalance
  val usedPercentage: BigDecimal = if (GuaranteeLimit.compare(zero) != 0) usedFunds / GuaranteeLimit * 100 else zero
}

case class CDSCashBalance(AvailableAccountBalance: Option[BigDecimal]) extends Balances

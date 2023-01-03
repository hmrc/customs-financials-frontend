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

package services

import javax.inject.{Inject, Singleton}
import domain.FileRole
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(apiService: ApiService)(implicit ec: ExecutionContext) {

  def fetchNotifications(eori: String)(implicit hc: HeaderCarrier): Future[Seq[Notification]] =
    apiService.getEnabledNotifications(eori).map(_.map(notification => Notification(notification.fileRole, notification.isRequested)))
}

case class Notification(fileRole: FileRole, isRequested: Boolean)

import play.sbt.PlayImport.ws
import sbt.*

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

object AppDependencies {
  val bootstrapVersion = "9.19.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-partials-play-30"      % "10.2.0",
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.21.0",
    ws,
    "org.typelevel"     %% "cats-core"                  % "2.13.0",
    "uk.gov.hmrc"       %% "tax-year"                   % "5.0.0",
    "org.webjars.npm"    % "moment"                     % "2.30.1",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % "2.10.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.jsoup"          % "jsoup"                  % "1.21.2"         % Test,
    "org.scalatestplus" %% "mockito-4-11"           % "3.2.18.0"       % Test
  )
}

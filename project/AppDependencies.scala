/*
 * Copyright 2024 HM Revenue & Customs
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
import sbt.*

object AppDependencies {

  private val playBootstrapVersion = "9.11.0"
  private val mongoVersion = "2.6.0"

  private val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"        % playBootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"               % mongoVersion,
    "uk.gov.hmrc"             %% "domain-play-30"                   % "11.0.0"
  )

  private val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % playBootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"  % mongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test

}

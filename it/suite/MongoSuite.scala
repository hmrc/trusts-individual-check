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

package suite

import com.typesafe.config.ConfigFactory
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import uk.gov.hmrc.mongo.test.MongoSupport

trait MongoSuite extends MongoSupport {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(500, Millis))

  private lazy val config = Configuration(ConfigFactory.load(System.getProperty("config.resource")))

  val connectionString: String = config.get[String]("mongodb.uri")

  def dropTheDatabase(): Unit = {
    mongoDatabase.drop()
  }

  lazy val createApplication: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

}

/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import config.AppConfig
import models.{IdMatchRequest, IdMatchResponse}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
//import org.mockito.Matchers.any
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import play.api.{Configuration, Environment}
import services.IdentityMatchService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future


class IndividualCheckControllerSpec extends AnyWordSpec with MockitoSugar with Matchers with GuiceOneAppPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout{

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val service = mock[IdentityMatchService]

  when(service.matchId(any(), any(), any(), any(),  any())(any(), any())).thenReturn(Future.successful(IdMatchResponse("ID", true)))

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[IdentityMatchService].toInstance(service)).build()

  "individualCheck /" should {
    "return runtime exception" in {

      val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
        .withJsonBody(Json.toJson(IdMatchRequest("ID", "AB123456A", "Name", "Name", "2000-01-01")))

      val result = route(app, request)

      println(result)
      println(await(result.get))
    }
  }
}

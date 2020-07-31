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

import models.{IdMatchError, IdMatchRequest, IdMatchResponse}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HttpClient
import util.IdentityMatchHelper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import services.IdentityMatchService

import scala.concurrent.Future


class IndividualCheckControllerSpec extends AnyWordSpec with IdentityMatchHelper with Matchers with GuiceOneAppPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout{

  private val service = mock[IdentityMatchService]

  when(service.matchId(any())(any(), any())).thenReturn(Future.successful(Right(IdMatchResponse("ID", true))))

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(bind[IndividualCheckRepository].toInstance(repository))
    .overrides(bind[HttpClient].toInstance(httpClient)).build()

  "IndividualCheckController" should {

    "return a response to a valid request" in {

      val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
        .withJsonBody(Json.toJson(successRequest))

      val result = route(app, request)

      val jsResult = Json.parse(contentAsString(result.get)).validate[IdMatchResponse]

      jsResult.isSuccess mustBe true

      jsResult.get mustBe successResponse
    }
  }

  "return a response to an invalid request" in {

    val requestWithInvalidNino = IdMatchRequest(id = idString, nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

    val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
      .withJsonBody(Json.toJson(requestWithInvalidNino))

    val result = route(app, request)

    val jsResult = Json.parse(contentAsString(result.get)).validate[IdMatchError]

    jsResult.isSuccess mustBe true

    jsResult.get.errors.contains("Could not validate the request") mustBe true
  }

  "return a generic response if API sends an error" in {

    val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
      .withJsonBody(Json.toJson(errorRequest))

    val result = route(app, request)

    val jsResult = Json.parse(contentAsString(result.get)).validate[IdMatchError]

    jsResult.isSuccess mustBe true

    jsResult.get.errors.contains("Something went wrong") mustBe true
  }

  "return a specific response if API limit is reached" in {

    val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
      .withJsonBody(Json.toJson(maxAttemptsRequest))

    val result = route(app, request)

    val jsResult = Json.parse(contentAsString(result.get)).validate[IdMatchError]

    jsResult.isSuccess mustBe true

    jsResult.get.errors.contains("Individual check - retry limit reached (3)") mustBe true
  }
}

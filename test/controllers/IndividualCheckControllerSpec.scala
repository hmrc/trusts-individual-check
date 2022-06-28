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

package controllers

import models.{IdMatchRequest, IdMatchResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import repositories.IndividualCheckRepository
import services.IdentityMatchService
import uk.gov.hmrc.http.HttpClient
import util.{BaseSpec, IdentityMatchHelper}

import scala.concurrent.Future

class IndividualCheckControllerSpec extends BaseSpec with IdentityMatchHelper with FutureAwaits with DefaultAwaitTimeout with Matchers {

  private val service = mock[IdentityMatchService]

  private val id = "ID"

  when(service.matchId(any())(any(), any()))
    .thenReturn(Future.successful(Right(IdMatchResponse(id, idMatch = true))))

  override lazy val app: Application = applicationBuilder()
    .overrides(bind[IndividualCheckRepository].toInstance(mockIndividualCheckRepository))
    .overrides(bind[HttpClient].toInstance(httpClient)).build()

  "IndividualCheckController" when {

    ".individualCheck" should {

      "return a response to a valid request" in {

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(successRequest))

        val result = route(app, request).get

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.toJson(successResponse)
      }

      "return a response to an invalid request" in {

        val requestWithInvalidNino = IdMatchRequest(id = idString, nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(requestWithInvalidNino))

        val result = route(app, request).get

        status(result) mustBe BAD_REQUEST

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "Could not validate the request"
            | ]
            |}""".stripMargin))
      }

      "return not found if the API is unable to locate the nino" in {

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(notFoundRequest))

        val result = route(app, request).get

        status(result) mustBe NOT_FOUND

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "Dependent service indicated that no data can be found"
            | ]
            |}""".stripMargin))
      }

      "return a service unavailable if API sends 503" in {

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(serviceUnavailableRequest))

        val result = route(app, request).get

        status(result) mustBe SERVICE_UNAVAILABLE

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "Dependent service is unavailable"
            | ]
            |}""".stripMargin))
      }

      "return a internal server error if API sends 500" in {

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(internalServerErrorRequest))

        val result = route(app, request).get

        status(result) mustBe INTERNAL_SERVER_ERROR

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "IF is currently experiencing problems that require live service intervention"
            | ]
            |}""".stripMargin))
      }

      "return a specific response if API limit is reached" in {

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck.url)
          .withJsonBody(Json.toJson(maxAttemptsRequest))

        val result = route(app, request).get

        status(result) mustBe FORBIDDEN

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "Individual check - retry limit reached (3)"
            | ]
            |}""".stripMargin))
      }
    }

    ".failedAttempts" should {

      "return current failed attempt count for given id" in {

        val numberOfFailedAttempts: Int = 1

        when(mockIndividualCheckRepository.getCounter(any()))
          .thenReturn(Future.successful(numberOfFailedAttempts))

        val request = FakeRequest(GET, routes.IndividualCheckController.failedAttempts(id).url)

        val result = route(app, request).get

        status(result) mustBe OK

        contentAsJson(result) mustBe Json.toJson(numberOfFailedAttempts)
      }
    }
  }
}

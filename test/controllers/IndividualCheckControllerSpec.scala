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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import models.{IdMatchRequest, IdMatchResponse, OperationSucceeded}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import util.{BaseSpec, IdentityMatchHelper}

import scala.concurrent.Future

class IndividualCheckControllerSpec extends BaseSpec
  with IdentityMatchHelper
  with FutureAwaits
  with DefaultAwaitTimeout {

  val individualsMatchUrl = "/individuals/match"

  when(mockIndividualCheckRepository.incrementCounter(any())).thenReturn(Future.successful(OperationSucceeded))
  when(mockIndividualCheckRepository.getCounter(any())).thenReturn(Future.successful(0))
  when(mockIndividualCheckRepository.clearCounter(any())).thenReturn(Future.successful(OperationSucceeded))

  "IndividualCheckController" when {

    ".individualCheck" should {

      "return a response to a valid request" in {

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchSuccess.toString())
          )
        )

        val individualCheckUrl = routes.IndividualCheckController.individualCheck().url
        val request =
          FakeRequest(POST, individualCheckUrl)
            .withJsonBody(Json.toJson(genericIdMatchRequest))

        val result = route(application, request).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(IdMatchResponse(idString, idMatch = true))
      }

      "return a response to an invalid request" in {

        val requestWithInvalidNino = IdMatchRequest(id = idString, nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
          .withJsonBody(Json.toJson(requestWithInvalidNino))

        val result = route(application, request).get

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

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
          .withJsonBody(Json.toJson(genericIdMatchRequest))

        val result = route(application, request).get

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

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
          .withJsonBody(Json.toJson(genericIdMatchRequest))

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(SERVICE_UNAVAILABLE)
          )
        )

        val result = route(application, request).get

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

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
          .withJsonBody(Json.toJson(genericIdMatchRequest))

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
        )

        val result = route(application, request).get

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

        val counterOverMaxAttempts = 5
        when(mockIndividualCheckRepository.getCounter(any())).thenReturn(Future.successful(counterOverMaxAttempts))

        val request = FakeRequest(POST, routes.IndividualCheckController.individualCheck().url)
          .withJsonBody(Json.toJson(genericIdMatchRequest))

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(FORBIDDEN)
          )
        )

        val result = route(application, request).get

        status(result) mustBe FORBIDDEN

        contentAsJson(result) mustBe Json.toJson(Json.parse(
          """
            |{
            | "errors": [
            |   "Individual check - retry limit reached (3)"
            | ]
            |}""".stripMargin))
      }


      ".failedAttempts" should {

        "return current failed attempt count for given id" in {

          val id = "ID"

          val url = s" /$id/failed-attempts"

          val numberOfFailedAttempts: Int = 1

          wireMockServer.stubFor(
            post(urlEqualTo(url)).willReturn(
              aResponse()
                .withStatus(OK)
            )
          )

          when(mockIndividualCheckRepository.getCounter(any()))
            .thenReturn(Future.successful(numberOfFailedAttempts))

          val request = FakeRequest(GET, routes.IndividualCheckController.failedAttempts(id).url)

          val result = route(application, request).get

          status(result) mustBe OK

          contentAsJson(result) mustBe Json.toJson(numberOfFailedAttempts)
        }
      }
    }
  }
}

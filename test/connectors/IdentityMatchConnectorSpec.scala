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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import exceptions.{InvalidIdMatchRequest, InvalidIdMatchResponse}
import models.api1585.{IdMatchApiRequest, IdMatchApiResponseError, IdMatchApiResponseSuccess}
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.{CONTENT_TYPE, await}
import uk.gov.hmrc.http.HeaderCarrier
import util.IdentityMatchHelper

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityMatchConnectorSpec extends AnyWordSpec  with IdentityMatchHelper
                                                      with Matchers
                                                      with GuiceOneAppPerSuite
                                                      with ScalaFutures
                                                      with DefaultAwaitTimeout
                                                      with WireMockHelper
  with IntegrationPatience
  with EitherValues {

  private def applicationBuilder(): GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      Seq(
        "microservice.services.individual-match.port" -> server.port()
      ): _*
    )

  private lazy val application = applicationBuilder().build()

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  private def identityMatchConnector = application.injector.instanceOf[IdentityMatchConnector]

  "Identity Match Connector" should {

    "parse response correctly" when {

      "successful response is returned from the API" in {

        server.stubFor(post(urlEqualTo("/individuals/match"))
          .withHeader(CONTENT_TYPE, containing("application/json"))
          .withHeader("Environment", containing("dev"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchSuccessBody)))

        val result =
          identityMatchConnector.matchId(successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate)

        whenReady(result) {
          r =>
            r.right.value mustBe IdMatchApiResponseSuccess(true)
        }
      }

      "error response is returned from the API" in {

        server.stubFor(post(urlEqualTo("/individuals/match"))
          .withHeader(CONTENT_TYPE, containing("application/json"))
          .withHeader("Environment", containing("dev"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(matchErrorBody)))

        val result: Future[Either[IdMatchApiResponseError, IdMatchApiResponseSuccess]] =
          identityMatchConnector.matchId(errorRequest.nino, errorRequest.surname, errorRequest.forename, errorRequest.birthDate)

        whenReady(result) {
          r =>
            r.left.value mustBe matchError.as[IdMatchApiResponseError]
        }
      }

      "internal server error is returned from the API" in {

        server.stubFor(post(urlEqualTo("/individuals/match"))
          .withHeader(CONTENT_TYPE, containing("application/json"))
          .withHeader("Environment", containing("dev"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody(internalServerErrorBody)))

        val result: Future[Either[IdMatchApiResponseError, IdMatchApiResponseSuccess]] =
          identityMatchConnector.matchId(errorRequest.nino, errorRequest.surname, errorRequest.forename, errorRequest.birthDate)

        whenReady(result) {
          r =>
            r.left.value mustBe internalServerError.as[IdMatchApiResponseError]
        }
      }

      "throw an exception" when {

        "the request fails validation" in {

          val requestWithInvalidNino = IdMatchApiRequest(nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

          val caught = intercept[InvalidIdMatchRequest] {
            identityMatchConnector.matchId(
              requestWithInvalidNino.nino, requestWithInvalidNino.surname, requestWithInvalidNino.forename, requestWithInvalidNino.birthDate).futureValue
          }

          caught.getMessage mustBe "Could not validate the request"
        }

        "the response fails validation" in {

          val invalidRequest: IdMatchApiRequest = IdMatchApiRequest("AB123456C", "Name", "Name", "2000-01-01")

          server.stubFor(post(urlEqualTo("/individuals/match"))
            .withHeader(CONTENT_TYPE, containing("application/json"))
            .withHeader("Environment", containing("dev"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody("""{"invalid": "invalid response"}""".stripMargin)))

          val caught = intercept[InvalidIdMatchResponse] {
            await(identityMatchConnector.matchId(invalidRequest.nino, invalidRequest.surname, invalidRequest.forename, invalidRequest.birthDate))
          }

          caught.getMessage mustBe "Could not validate the response"
        }
      }
    }
  }
}

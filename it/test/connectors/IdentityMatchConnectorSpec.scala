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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import exceptions.InvalidIdMatchRequest
import models.IdMatchRequest
import models.api1585._
import org.scalatest.EitherValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers.CONTENT_TYPE
import suite.BaseSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import util.BaseSpec

import scala.concurrent.Future

class IdentityMatchConnectorSpec extends BaseSpec with BaseSuite
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures
  with DefaultAwaitTimeout
  with WireMockSupport
  with IntegrationPatience
  with EitherValues {

  val individualsMatchUrl = "/individuals/match"

  private def applicationBuilder(): GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.individual-match.port" -> wireMockServer.port(),
      "mongodb.uri" -> "mongodb://localhost:27017/individual-check-it",
      "metrics.enabled" -> false,
      "auditing.enabled" -> false
    )

  private lazy val application = applicationBuilder().build()

  implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()

  private def identityMatchConnector = application.injector.instanceOf[IdentityMatchConnector]

  private def getMatchIdResponse(request: IdMatchRequest, connector: IdentityMatchConnector): Future[IdMatchApiResponse] = {
    connector.matchId(
      request.nino,
      request.surname,
      request.forename,
      request.birthDate
    )
  }

  "Identity Match Connector" should {

    "parse response correctly" when {

      "successful response is returned from the API" in {

        createMockForIndividualMatchUrlWithHeaders(OK, matchSuccessBody, individualsMatchUrl)

        val result = getMatchIdResponse(genericIdMatchRequest, identityMatchConnector)

        result.futureValue mustBe IdMatchApiResponseSuccess(true)
      }

      "error response is returned from the API" in {

        createMockForIndividualMatchUrlWithHeaders(NOT_FOUND, matchErrorBody, individualsMatchUrl)

        val result = getMatchIdResponse(genericIdMatchRequest, identityMatchConnector)

        result.futureValue mustBe NinoNotFound
      }

      "internal server error is returned from the API" in {

        createMockForIndividualMatchUrlWithHeaders(INTERNAL_SERVER_ERROR, internalServerErrorBody, individualsMatchUrl)

        val result = getMatchIdResponse(genericIdMatchRequest, identityMatchConnector)

        result.futureValue mustBe DownstreamServerError
      }

      "service unavailable is returned from the API" in {

        createMockForIndividualMatchUrlWithHeaders(SERVICE_UNAVAILABLE, matchErrorBody, individualsMatchUrl)

        val result = getMatchIdResponse(genericIdMatchRequest, identityMatchConnector)

        result.futureValue mustBe DownstreamServiceUnavailable
      }

      "throw an exception" when {

        "the request fails validation" in {

          val requestWithInvalidNino =
            IdMatchApiRequest(nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

          val caught = intercept[InvalidIdMatchRequest] {
            identityMatchConnector.matchId(
              requestWithInvalidNino.nino,
              requestWithInvalidNino.surname,
              requestWithInvalidNino.forename,
              requestWithInvalidNino.birthDate
            ).futureValue
          }

          caught.getMessage mustBe "Could not validate the request"
        }
      }
    }
  }
}

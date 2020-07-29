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

import config.AppConfig
import exceptions.{InvalidIdMatchRequest, InvalidIdMatchResponse}
import models.{IdMatchApiRequest, IdMatchApiResponseFailure, IdMatchApiResponseSuccess}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityMatchConnectorSpec extends AnyWordSpec  with MockitoSugar
                                                      with Matchers
                                                      with GuiceOneAppPerSuite
                                                      with FutureAwaits
                                                      with DefaultAwaitTimeout {

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val httpClient = mock[HttpClient]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val identityMatchConnector = new IdentityMatchConnector(httpClient, appConfig)

  "Identity Match Connector" should {

    val successRequest = IdMatchApiRequest("AB123456A", "Name", "Name", "2000-01-01")

    val successResponse:JsValue = Json.parse("""{"individualMatch":false}""")

    val failureRequest = IdMatchApiRequest("AB123456B", "Name", "Name", "2000-01-01")

    val failureResponse:JsValue = Json.parse("""{"failures":[{
                                                 |"code":"RESOURCE_NOT_FOUND",
                                                 |"reason":"The remote endpoint has indicated that no data can be found."}]}""".stripMargin)

    val invalidRequest = IdMatchApiRequest(nino = "INVALID", forename = "Name", surname = "Name", birthDate = "2000-01-01")

    val invalidResponse: JsValue = JsString("")

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
    } thenReturn Future(invalidResponse)

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), mockEq(successRequest), any())(any(), any(), any(), any())
    } thenReturn Future(successResponse)

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), mockEq(failureRequest), any())(any(), any(), any(), any())
    } thenReturn Future(failureResponse)

    "parse response correctly" when {

      "successful response is returned from the API" in {

        val result: Either[IdMatchApiResponseFailure, IdMatchApiResponseSuccess] =
          await(identityMatchConnector.matchId(successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate))

        result.isRight mustBe true
        result.isLeft mustBe false
        result.right.get mustBe successResponse.as[IdMatchApiResponseSuccess]
      }

      "error response is returned from the API" in {

        val result: Either[IdMatchApiResponseFailure, IdMatchApiResponseSuccess] =
          await(identityMatchConnector.matchId(failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate))

        result.isLeft mustBe true
        result.isRight mustBe false
        result.left.get mustBe failureResponse.as[IdMatchApiResponseFailure]
      }
    }

    "throw an exception" when {

      "the request fails validation" in {

        val caught = intercept[InvalidIdMatchRequest] {
          await(identityMatchConnector.matchId(invalidRequest.nino, invalidRequest.surname, invalidRequest.forename, invalidRequest.birthDate))
        }

        caught.getMessage mustBe "Could not validate the request"
      }

      "the response fails validation" in {

        val caught = intercept[InvalidIdMatchResponse] {
          await(identityMatchConnector.matchId("AB123456C", "Name", "Name", "2000-01-01"))
        }

        caught.getMessage mustBe "Could not validate the response"
      }
    }
  }
}

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
import models.{IdMatchApiResponseFailure, IdMatchApiResponseSuccess, IdMatchRequest}
import org.mockito.ArgumentMatchers.any
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

class IdentityMatchConnectorSpec extends AnyWordSpec with MockitoSugar with Matchers with GuiceOneAppPerSuite with FutureAwaits with DefaultAwaitTimeout{

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val httpClient = mock[HttpClient]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  val identityMatchConnector = new IdentityMatchConnector(httpClient, appConfig)

  private val exampleSuccessJson:String = "{\"individualMatch\":false}"

  private val exampleErrorJson:String = "{\"failures\":[{\"code\":\"RESOURCE_NOT_FOUND\",\"reason\":\"The remote endpoint has indicated that no data can be found.\"}]}"

  val success:JsValue = Json.parse(exampleSuccessJson)
  val failure:JsValue = Json.parse(exampleErrorJson)

  "Identity Match Connector" should {

    "parse response correctly" when {

      "success is returned" in {

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(success)

        val v: Either[IdMatchApiResponseFailure, IdMatchApiResponseSuccess] =
          await(identityMatchConnector.matchId("AB123456A", "Name", "Name", "2000-01-01"))

        v.isRight mustBe true
        v.right.get mustBe success.as[IdMatchApiResponseSuccess]
      }

      "failure is returned" in {

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(failure)

        val v: Either[IdMatchApiResponseFailure, IdMatchApiResponseSuccess] =
          await(identityMatchConnector.matchId("AB123456A", "Name", "Name", "2000-01-01"))

        v.isLeft mustBe true
        v.left.get mustBe failure.as[IdMatchApiResponseFailure]
      }
    }

    "throw an exception" when {

      "request is invalid" in {

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(failure)

        val caught = intercept[InvalidIdMatchRequest] {
          await(identityMatchConnector.matchId("", "", "", ""))
        }

        caught.getMessage mustBe "Could not validate the request"
      }

    "response is invalid" in {

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(JsString(""))

        val caught = intercept[InvalidIdMatchResponse] {
          await(identityMatchConnector.matchId("AB123456A", "Name", "Name", "2000-01-01"))
        }

        caught.getMessage mustBe "Could not validate the response"
      }
    }
  }
}

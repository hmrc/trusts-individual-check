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

package services

import config.AppConfig
import connectors.IdentityMatchConnector
import exceptions.{InvalidIdMatchRequest, InvalidIdMatchResponse, LimitException}
import models.{IdMatchApiResponseFailure, IdMatchApiResponseSuccess, IdMatchRequest, IdMatchResponse, OperationSucceeded}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{times, verify, when, reset}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class IdentityMatchServiceSpec extends AnyWordSpec with MockitoSugar with Matchers with GuiceOneAppPerSuite with FutureAwaits with DefaultAwaitTimeout{

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val httpClient = mock[HttpClient]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val repository = mock[IndividualCheckRepository]

  val identityMatchConnector = new IdentityMatchConnector(httpClient, appConfig)
  val identityMatchService = new IdentityMatchService(identityMatchConnector, repository, appConfig)

  private val exampleSuccessJson:String = "{\"individualMatch\":true}"
  private val exampleErrorJson:String = "{\"failures\":[{\"code\":\"RESOURCE_NOT_FOUND\",\"reason\":\"The remote endpoint has indicated that no data can be found.\"}]}"

  val success:JsValue = Json.parse(exampleSuccessJson)
  val failure:JsValue = Json.parse(exampleErrorJson)

  val idString = "IDSTRING"

  "Identity Match Connector" should {

    "parse response correctly" when {

      "success is returned" in {

        when {
          repository.getCounter(any())
        } thenReturn (Future.successful(0))

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(success)

        val v: IdMatchResponse =
          await(identityMatchService.matchId(idString, "AB123456A", "Name", "Name", "2000-01-01"))

        v.id mustBe idString
        v.idMatch mustBe true
      }


    }

    "reset the counter on success" in {

        var counter = 0

        val intCapture:ArgumentCaptor[Int] = ArgumentCaptor.forClass(classOf[Int])

        when {
          repository.setCounter(any(), intCapture.capture())
        } thenReturn {
          counter += 1
          Future.successful( {
          OperationSucceeded
        })}

        when {
          repository.getCounter(any())
        } thenReturn (Future.successful(counter))


        reset(httpClient)
        reset(repository)

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(failure)

        Try {
          val attempt1: IdMatchResponse =
            await(identityMatchService.matchId(idString, "AB123456B", "Name", "Name", "2000-01-01"))
        }

        Try {
          val attempt2: IdMatchResponse =
            await(identityMatchService.matchId(idString, "AB123456B", "Name", "Name", "2000-01-01"))
        }

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(success)

        val attempt3: IdMatchResponse =
          await(identityMatchService.matchId(idString, "AB123456B", "Name", "Name", "2000-01-01"))

        verify(repository, times(1)).setCounter(any(), mockEq(0))
      }


    "throw exception" when {

      "failure is returned" in {

        when {
          repository.getCounter(any())
        } thenReturn (Future.successful(0))

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(failure)


        val caught = intercept[Exception] {
          await(identityMatchService.matchId(idString, "AB123456A", "Name", "Name", "2000-01-01"))
        }

        caught.getMessage mustBe "Something went wrong"
      }

      "maximum attempts is reached" in {

        when {
          repository.getCounter(any())
        } thenReturn (Future.successful(3))

        when {
          httpClient.POST[IdMatchRequest, JsValue](any(), any(), any())(any(), any(), any(), any())
        } thenReturn Future(success)


        val caught = intercept[LimitException] {
          await(identityMatchService.matchId(idString, "AB123456A", "Name", "Name", "2000-01-01"))
        }

        caught.getMessage mustBe "Individual check - retry limit reached (3)"
      }
    }
  }
}

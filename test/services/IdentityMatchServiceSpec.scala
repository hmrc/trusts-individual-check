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
import exceptions.LimitException
import models.{IdMatchApiRequest, IdMatchResponse}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityMatchServiceSpec extends AnyWordSpec  with MockitoSugar
                                                    with Matchers
                                                    with GuiceOneAppPerSuite
                                                    with FutureAwaits
                                                    with DefaultAwaitTimeout
                                                    with BeforeAndAfterEach {

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  private val httpClient = mock[HttpClient]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  private val repository = mock[IndividualCheckRepository]

  val identityMatchConnector = new IdentityMatchConnector(httpClient, appConfig)

  val identityMatchService = new IdentityMatchService(identityMatchConnector, repository, appConfig)

  val idString = "IDSTRING"

  val maxAttemptsIdString = "MAX ATTEMPTS"

  override def beforeEach(): Unit = {

    reset(repository)

    when {
      repository.getCounter(idString)
    } thenReturn (Future.successful(0))

    when {
      repository.getCounter(maxAttemptsIdString)
    } thenReturn (Future.successful(3))
  }

  "Identity Match Connector" should {

    val matchSuccess:JsValue = Json.parse("""{"individualMatch":true}""")

    val matchFailure:JsValue = Json.parse("""{"individualMatch":false}""")

    val matchError:JsValue = Json.parse("""{"failures":[{
                                         |"code":"RESOURCE_NOT_FOUND",
                                         |"reason":"The remote endpoint has indicated that no data can be found."}]}""".stripMargin)

    val successRequest = IdMatchApiRequest("AB123456A", "Name", "Name", "2000-01-01")

    val failureRequest = IdMatchApiRequest("AB123456B", "Name", "Name", "2000-01-01")

    val errorRequest = IdMatchApiRequest("AB123456C", "Name", "Name", "2000-01-01")

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), mockEq(successRequest), any())(any(), any(), any(), any())
    } thenReturn Future(matchSuccess)

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), mockEq(failureRequest), any())(any(), any(), any(), any())
    } thenReturn Future(matchFailure)

    when {
      httpClient.POST[IdMatchApiRequest, JsValue](any(), mockEq(errorRequest), any())(any(), any(), any(), any())
    } thenReturn Future(matchError)

    "parse response correctly" when {

      "success is returned" in {

        val result: IdMatchResponse = await(identityMatchService.matchId(
          idString, successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate))

        result.id mustBe idString
        result.idMatch mustBe true
      }
    }

    "reset the counter on success" in {

      val attempt1: IdMatchResponse = await(identityMatchService.matchId(
        idString, failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate))

      attempt1.idMatch mustBe false

      verify(repository, times(1)).incrementCounter(mockEq(idString))

      val attempt2: IdMatchResponse = await(identityMatchService.matchId(
        idString, failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate))

      attempt2.idMatch mustBe false

      verify(repository, times(2)).incrementCounter(mockEq(idString))

      val attempt3: IdMatchResponse = await(identityMatchService.matchId(
        idString, successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate))

      attempt3.idMatch mustBe true

      verify(repository, times(1)).clearCounter(mockEq(idString))
    }

    "throw exception" when {

      "error is returned" in {

        val caught = intercept[Exception] {
          await(identityMatchService.matchId(
            idString, errorRequest.nino, errorRequest.surname, errorRequest.forename, errorRequest.birthDate))
        }

        caught.getMessage mustBe "Something went wrong"
      }

      "maximum attempts is reached" in {

        val caught = intercept[LimitException] {
          await(identityMatchService.matchId(
            maxAttemptsIdString, successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate))
        }

        caught.getMessage mustBe "Individual check - retry limit reached (3)"
      }
    }
  }
}

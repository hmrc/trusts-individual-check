/*
 * Copyright 2021 HM Revenue & Customs
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

package util

import models.{IdMatchRequest, IdMatchResponse, OperationSucceeded}
import models.api1585.{DownstreamServerError, DownstreamServiceUnavailable, IdMatchApiRequest, IdMatchApiResponse, IdMatchApiResponseSuccess, NinoNotFound}
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.scalatest.{BeforeAndAfterEach, Suite}
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait IdentityMatchHelper extends MockitoSugar with BeforeAndAfterEach { this: Suite =>

  val httpClient:HttpClient = mock[HttpClient]

  val mockIndividualCheckRepository:IndividualCheckRepository = mock[IndividualCheckRepository]

  val idString = "IDSTRING"

  val maxAttemptsIdString = "MAX ATTEMPTS"

  val matchSuccessBody = """{"individualMatch":true}"""

  val matchSuccess:JsValue = Json.parse(matchSuccessBody)

  val matchFailure:JsValue = Json.parse("""{"individualMatch":false}""")

  val internalServerErrorBody:String = """{
                                  |  "failures": [
                                  |    {
                                  |      "code": "SERVER_ERROR",
                                  |      "reason": "IF is currently experiencing problems that require live service intervention."
                                  |    }
                                  |  ]
                                  |}""".stripMargin

  val serviceUnavailableErrorBody:String = """{
                                             |  "failures": [
                                             |    {
                                             |      "code": "SERVICE_UNAVAILABLE",
                                             |      "reason": "Dependent systems are currently not responding."
                                             |    }
                                             |  ]
                                             |}""".stripMargin


  val matchErrorBody:String = """{"failures":[{
                                |"code":"RESOURCE_NOT_FOUND",
                                |"reason":"The remote endpoint has indicated that no data can be found."}]}""".stripMargin

  val matchError:JsValue = Json.parse(matchErrorBody)

  val internalServerError:JsValue = Json.parse(internalServerErrorBody)

  val serviceUnavailableError:JsValue = Json.parse(serviceUnavailableErrorBody)

  val successRequest:IdMatchRequest = IdMatchRequest(idString, "AB123456A", "Name", "Name", "2000-01-01")

  val successApiRequest:IdMatchApiRequest = IdMatchApiRequest(successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate)

  val successResponse:IdMatchResponse = IdMatchResponse(idString, true)

  val failureRequest:IdMatchRequest = IdMatchRequest(idString, "AB123456B", "Name", "Name", "2000-01-01")

  val failureApiRequest:IdMatchApiRequest = IdMatchApiRequest(failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate)

  val failureResponse:IdMatchResponse = IdMatchResponse(idString, false)

  val notFoundRequest:IdMatchRequest = IdMatchRequest(idString, "AB123456C", "Name", "Name", "2000-01-01")

  val serviceUnavailableRequest:IdMatchRequest = IdMatchRequest(idString, "AB123456C", "Unavailable", "Service", "2000-01-01")

  val internalServerErrorRequest:IdMatchRequest = IdMatchRequest(idString, "AB123456C", "Error", "Service", "2000-01-01")

  val notFoundApiRequest:IdMatchApiRequest = IdMatchApiRequest(notFoundRequest.nino, notFoundRequest.surname, notFoundRequest.forename, notFoundRequest.birthDate)

  val serviceUnavailableApiRequest:IdMatchApiRequest = IdMatchApiRequest(serviceUnavailableRequest.nino, "Unavailable", "Service", serviceUnavailableRequest.birthDate)

  val internalServerErrorApiRequest:IdMatchApiRequest = IdMatchApiRequest(internalServerErrorRequest.nino, "Error", "Service", internalServerErrorRequest.birthDate)

  val maxAttemptsRequest:IdMatchRequest = IdMatchRequest(maxAttemptsIdString, "AB123456A", "Name", "Name", "2000-01-01")


  override def beforeEach(): Unit = {

    reset(httpClient)
    reset(mockIndividualCheckRepository)

    when {
      mockIndividualCheckRepository.getCounter(idString)
    } thenReturn Future.successful(0)

    when {
      mockIndividualCheckRepository.getCounter(maxAttemptsIdString)
    } thenReturn Future.successful(3)

    when {
      mockIndividualCheckRepository.incrementCounter(any())
    } thenReturn Future.successful(OperationSucceeded)

    when {
      mockIndividualCheckRepository.clearCounter(any())
    } thenReturn Future.successful(OperationSucceeded)

    when {
      httpClient.POST[IdMatchApiRequest, IdMatchApiResponse](any(), mockEq(successApiRequest), any())(any(), any(), any(), any())
    } thenReturn Future(matchSuccess.as[IdMatchApiResponseSuccess])

    when {
      httpClient.POST[IdMatchApiRequest, IdMatchApiResponse](any(), mockEq(failureApiRequest), any())(any(), any(), any(), any())
    } thenReturn Future(matchFailure.as[IdMatchApiResponseSuccess])

    when {
      httpClient.POST[IdMatchApiRequest, IdMatchApiResponse](any(), mockEq(notFoundApiRequest), any())(any(), any(), any(), any())
    } thenReturn Future(NinoNotFound)

    when {
      httpClient.POST[IdMatchApiRequest, IdMatchApiResponse](any(), mockEq(serviceUnavailableApiRequest), any())(any(), any(), any(), any())
    } thenReturn Future(DownstreamServiceUnavailable)

    when {
      httpClient.POST[IdMatchApiRequest, IdMatchApiResponse](any(), mockEq(internalServerErrorApiRequest), any())(any(), any(), any(), any())
    } thenReturn Future(DownstreamServerError)
  }
}

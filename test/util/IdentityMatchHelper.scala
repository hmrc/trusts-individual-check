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

package util

import config.AppConfig
import models.api1585._
import models.{IdMatchRequest, IdMatchResponse, OperationSucceeded}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import repositories.IndividualCheckRepository
import services.AuditService
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import scala.concurrent.{ExecutionContext, Future}

trait IdentityMatchHelper extends MockitoSugar with BeforeAndAfterEach { this: Suite =>

  implicit lazy val ec: ExecutionContext = ExecutionContext.Implicits.global

  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockIndividualCheckRepository: IndividualCheckRepository = mock[IndividualCheckRepository]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAppConfig: AppConfig = mock[AppConfig]

  val idString = "IDSTRING"
  val maxAttemptsIdString = "MAX ATTEMPTS"

  val matchSuccessBody = """{"individualMatch":true}"""
  val matchSuccess: JsValue = Json.parse(matchSuccessBody)
  val matchFailure: JsValue = Json.parse("""{"individualMatch":false}""")

  val internalServerErrorBody: String =
    """{
      |  "failures": [
      |    {
      |      "code": "SERVER_ERROR",
      |      "reason": "IF is currently experiencing problems that require live service intervention."
      |    }
      |  ]
      |}""".stripMargin

  val serviceUnavailableErrorBody: String =
    """{
      |  "failures": [
      |    {
      |      "code": "SERVICE_UNAVAILABLE",
      |      "reason": "Dependent systems are currently not responding."
      |    }
      |  ]
      |}""".stripMargin


  val matchErrorBody: String =
    """{
      |  "failures": [
      |    {
      |      "code":"RESOURCE_NOT_FOUND",
      |      "reason":"The remote endpoint has indicated that no data can be found."
      |    }
      |  ]
      |}""".stripMargin

  val matchError: JsValue = Json.parse(matchErrorBody)

  val internalServerError: JsValue = Json.parse(internalServerErrorBody)

  val serviceUnavailableError: JsValue = Json.parse(serviceUnavailableErrorBody)

  val successRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456A", "Name", "Name", "2000-01-01")

  val successApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate)

  val successResponse: IdMatchResponse =
    IdMatchResponse(idString, idMatch = true)

  val failureRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456B", "Name", "Name", "2000-01-01")

  val failureApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate)

  val failureResponse: IdMatchResponse =
    IdMatchResponse(idString, idMatch = false)

  val notFoundRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456C", "Name", "Name", "2000-01-01")

  val serviceUnavailableRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456C", "Unavailable", "Service", "2000-01-01")

  val internalServerErrorRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456C", "Error", "Service", "2000-01-01")

  val notFoundApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(notFoundRequest.nino, notFoundRequest.surname, notFoundRequest.forename, notFoundRequest.birthDate)

  val serviceUnavailableApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(serviceUnavailableRequest.nino, "Unavailable", "Service", serviceUnavailableRequest.birthDate)

  val internalServerErrorApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(internalServerErrorRequest.nino, "Error", "Service", internalServerErrorRequest.birthDate)

  val maxAttemptsRequest:IdMatchRequest =
    IdMatchRequest(maxAttemptsIdString, "AB123456A", "Name", "Name", "2000-01-01")


  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
    reset(mockIndividualCheckRepository)
    reset(mockAuditService)

    when(mockIndividualCheckRepository.getCounter(idString)) thenReturn Future.successful(0)
    when(mockIndividualCheckRepository.getCounter(maxAttemptsIdString)) thenReturn Future.successful(3)
    when(mockIndividualCheckRepository.incrementCounter(any())) thenReturn Future.successful(OperationSucceeded)
    when(mockIndividualCheckRepository.clearCounter(any())) thenReturn Future.successful(OperationSucceeded)

    when(mockAppConfig.serviceUrl).thenReturn("http://localhost:1234")
    when(mockAppConfig.authBaseUrl).thenReturn("http://localhost:1234")

    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)


  }
}

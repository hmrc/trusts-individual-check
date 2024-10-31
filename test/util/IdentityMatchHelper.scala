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
import izumi.reflect.Tag
import models.api1585._
import models.{IdMatchRequest, IdMatchResponse, OperationSucceeded}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, when}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.BodyWritable
import repositories.IndividualCheckRepository
import services.AuditService
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

trait IdentityMatchHelper extends BaseSpec {


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
    IdMatchRequest(idString, "AB123456A", "Bob", "Success", "2000-01-01")

  val successApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(successRequest.nino, successRequest.surname, successRequest.forename, successRequest.birthDate)

  val successResponse: IdMatchResponse =
    IdMatchResponse(idString, idMatch = true)

  val failureRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456B", "Jim", "Failure", "2000-01-01")

  val failureApiRequest: IdMatchApiRequest =
    IdMatchApiRequest(failureRequest.nino, failureRequest.surname, failureRequest.forename, failureRequest.birthDate)

  val failureResponse: IdMatchResponse =
    IdMatchResponse(idString, idMatch = false)

  val notFoundRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456C", "Terry", "NotFound", "2000-01-01")

  // {"nino":"AB123456C","surname":"Name","forename":"Name","birthDate":"2000-01-01"}

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

  val maxAttemptsRequest: IdMatchRequest =
    IdMatchRequest(maxAttemptsIdString, "AB123456A", "Maximo", "Attemptio", "2000-01-01")


//  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
//  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
//  val mockIndividualCheckRepository: IndividualCheckRepository = mock[IndividualCheckRepository]
//  val mockAuditService: AuditService = mock[AuditService]
//  val mockAppConfig: AppConfig = mock[AppConfig]


//  def beforeEach(): Unit = {

//    reset(mockHttpClient)
//    reset(mockIndividualCheckRepository)
//    reset(mockRequestBuilder)
//    reset(mockAuditService)
//
//    when(mockIndividualCheckRepository.getCounter(idString)) thenReturn Future.successful(0)
//    when(mockIndividualCheckRepository.getCounter(maxAttemptsIdString)) thenReturn Future.successful(3)
//    when(mockIndividualCheckRepository.incrementCounter(any())) thenReturn Future.successful(OperationSucceeded)
//    when(mockIndividualCheckRepository.clearCounter(any())) thenReturn Future.successful(OperationSucceeded)
//
//    when(mockAppConfig.serviceUrl).thenReturn("http://localhost:1234")
//    when(mockAppConfig.authBaseUrl).thenReturn("http://localhost:1234")
//
//    when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
//    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
//    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)

//        val mockRequestBuilderWithMatchSuccess = mock[RequestBuilder]
        // this would need to be done in each test
//        when(mockRequestBuilder.execute[IdMatchApiResponseSuccess](any(), any()))
//          .thenReturn(Future.successful(matchSuccess.as[IdMatchApiResponseSuccess]))
//
//        when(
//          mockRequestBuilder.withBody(mockEq(Json.toJson(successApiRequest)))(any[BodyWritable[Any]], any[Tag[JsValue]], any[ExecutionContext])
//        ) thenReturn {
//          mockRequestBuilder
//        }
//
//     //2
//        when(mockRequestBuilder.execute[IdMatchApiResponseSuccess](any(), any()))
//          .thenReturn(Future.successful(matchFailure.as[IdMatchApiResponseSuccess]))
//
//        when {
//          mockRequestBuilder.withBody(mockEq(Json.toJson(failureApiRequest)))(any[BodyWritable[Any]], any[Tag[JsValue]], any[ExecutionContext])
//        } thenReturn mockRequestBuilder

    // 3
    //    val mockRequestBuilderWithNotFound = mock[RequestBuilder]
    //    when(mockRequestBuilderWithNotFound.execute[IdMatchApiResponse](any(), any()))
    //      .thenReturn(Future.successful(NinoNotFound))
    //
    //    when {
    //      mockRequestBuilder.withBody(mockEq(Json.toJson(notFoundApiRequest)))(any[BodyWritable[Any]], any[Tag[JsValue]], any[ExecutionContext])
    //    } thenReturn mockRequestBuilderWithNotFound


    // 4
//    val mockRequestBuilderWithDownstreamServiceUnavailable = mock[RequestBuilder]
//    when(mockRequestBuilderWithDownstreamServiceUnavailable.execute[IdMatchApiResponse](any(), any()))
//      .thenReturn(Future.successful(DownstreamServiceUnavailable))
//
//    when {
//      mockRequestBuilder.withBody(mockEq(Json.toJson(serviceUnavailableApiRequest)))(any[BodyWritable[Any]], any[Tag[JsValue]], any[ExecutionContext])
//    } thenReturn mockRequestBuilderWithDownstreamServiceUnavailable
//
//    // 5
//    val mockRequestBuilderWithDownstreamServerError = mock[RequestBuilder]
//    when(mockRequestBuilderWithDownstreamServerError.execute[IdMatchApiResponse](any(), any()))
//      .thenReturn(Future.successful(DownstreamServerError))
//
//    when {
//      mockRequestBuilder.withBody(mockEq(Json.toJson(serviceUnavailableApiRequest)))(any[BodyWritable[Any]], any[Tag[JsValue]], any[ExecutionContext])
//    } thenReturn mockRequestBuilderWithDownstreamServerError
//  }
}


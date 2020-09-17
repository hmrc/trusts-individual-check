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

import java.util.UUID

import config.AppConfig
import exceptions.{InvalidIdMatchRequest, InvalidIdMatchResponse}
import javax.inject.Inject
import uk.gov.hmrc.http.HttpReads.Implicits._
import models.api1585.{IdMatchApiRequest, IdMatchApiResponseError, IdMatchApiResponseSuccess}
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class IdentityMatchConnector @Inject()(val http: HttpClient, val appConfig: AppConfig) {

  private val logger = Logger("[IdentityMatchConnector]")

  private val postUrl = appConfig.idMatchEndpoint

  private val ENVIRONMENT_HEADER = "Environment"
  private val CORRELATION_HEADER = "CorrelationId"
  private val CONTENT_TYPE = "Content-Type"
  private val CONTENT_TYPE_JSON = "application/json; charset=utf-8"

  private def headers(correlationId : String) : Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION -> s"Bearer ${appConfig.idMatchToken}",
      CONTENT_TYPE -> CONTENT_TYPE_JSON,
      ENVIRONMENT_HEADER -> appConfig.idMatchEnv,
      CORRELATION_HEADER -> correlationId
    )

  def matchId(  nino: String, surname: String, forename: String, birthDate: String )
             (implicit ec: ExecutionContext): Future[Either[IdMatchApiResponseError, IdMatchApiResponseSuccess]] = {

    val request = IdMatchApiRequest(nino, surname, forename, birthDate)

    val correlationId = UUID.randomUUID().toString

    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = headers(correlationId))

    logger.info(s"Matching individual for correlationId: $correlationId")

    if(Json.toJson(request).validate[IdMatchApiRequest].isError) {
      throw new InvalidIdMatchRequest("Could not validate the request")
    }

    for {
      response <- http.POST[IdMatchApiRequest, JsValue](postUrl, request)
    } yield {
      val success = response.validate(IdMatchApiResponseSuccess.format)
      val failure = response.validate(IdMatchApiResponseError.format)
      (success, failure) match {
        case (JsSuccess(s, _), JsError(_)) => Right(s)
        case (JsError(_), JsSuccess(f, _)) => Left(f)
        case _ => throw new InvalidIdMatchResponse("Could not validate the response")
      }
    }
  }
}

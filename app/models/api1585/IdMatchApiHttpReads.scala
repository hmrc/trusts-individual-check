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

package models.api1585

import play.api.http.Status._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

sealed trait IdMatchApiResponse
sealed trait IdMatchApiError extends IdMatchApiResponse

final case class IdMatchApiResponseSuccess(individualMatch: Boolean) extends IdMatchApiResponse
final case class DownstreamBadRequest(reason : ErrorResponseDetail) extends IdMatchApiError
case object NinoNotFound extends IdMatchApiError
case object DownstreamServerError extends IdMatchApiError
case object DownstreamServiceUnavailable extends IdMatchApiError

object IdMatchApiResponseSuccess {

  implicit lazy val format: Format[IdMatchApiResponseSuccess] = Json.format[IdMatchApiResponseSuccess]
}

object IdMatchApiHttpReads {

  implicit lazy val httpReads : HttpReads[IdMatchApiResponse] = (_: String, _: String, response: HttpResponse) => {
    response.status match {
      case OK => response.json.as[IdMatchApiResponseSuccess]
      case BAD_REQUEST =>
        (response.json \ "failures").asOpt[ErrorResponseDetail] match {
          case None => DownstreamServerError
          case Some(value) => DownstreamBadRequest(value)
        }
      case NOT_FOUND => NinoNotFound
      case INTERNAL_SERVER_ERROR => DownstreamServerError
      case _ => DownstreamServiceUnavailable
    }
  }

}

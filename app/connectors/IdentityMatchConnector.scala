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

import config.AppConfig
import exceptions.InvalidIdMatchRequest
import models.api1585.IdMatchApiHttpReads.httpReads
import models.api1585.{IdMatchApiRequest, IdMatchApiResponse}
import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.{JsError, JsSuccess, Json}
import services.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import utils.Session

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IdentityMatchConnector @Inject()(
                                        http: HttpClientV2,
                                        auditService: AuditService,
                                        appConfig: AppConfig
                                      )(implicit ec: ExecutionContext) extends Logging {

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

  def matchId(nino: String, surname: String, forename: String, birthDate: String)
             (implicit hc: HeaderCarrier): Future[IdMatchApiResponse] = {

    val request = IdMatchApiRequest(nino, surname, forename, birthDate)

    val correlationId = UUID.randomUUID().toString

    logger.info(s"[Session ID: ${Session.id(hc)}] Matching individual for correlationId: $correlationId")
    auditService.auditOutboundCall(request)

    Json.toJson(request).validate[IdMatchApiRequest] match {
      case JsSuccess(validRequest, _) =>
        val x: RequestBuilder = http.post(url"$postUrl")

        val y = x.setHeader(headers(correlationId): _*)

         y
          .withBody(Json.toJson(validRequest))
          .execute[IdMatchApiResponse]

      case JsError(errors) =>
        logger.error(s"[Session ID: ${Session.id(hc)}] Unable to transform request for IFS due to ${JsError.toJson(errors)} for correlationId: $correlationId")
        throw new InvalidIdMatchRequest("Could not validate the request")
    }
  }
}

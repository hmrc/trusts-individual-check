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

package services

import models.IdMatchRequest
import models.auditing.{GetTrustAuditEvent, TrustAuditing}
import play.api.libs.json.{JsValue, Json, OWrites}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AuditService @Inject()(auditConnector: AuditConnector)(implicit ec: ExecutionContext){

  private def audit(event: String,
            request: JsValue,
            internalId: String,
            response: JsValue)(implicit hc: HeaderCarrier): Unit = {

    val auditPayload = GetTrustAuditEvent(
      request = request,
      internalId = internalId,
      response = response
    )

    auditConnector.sendExplicitAudit(
      event,
      auditPayload
    )
  }

  def auditIdentityMatchAttempt(idMatchRequest: IdMatchRequest,
                                count: Int,
                                idMatchResponse: String
                               )(implicit hc: HeaderCarrier): Unit = {

    val auditCount = count + 1

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino
    )

    val response = Json.obj(
      "response" -> idMatchResponse,
      "responseMsg" -> "Match attempt.",
      "countOfTheAttempt" -> auditCount,
      "isLocked" -> false
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT,
      request = request,
      internalId = idMatchRequest.id,
      response = response
    )
  }

  def auditIdentityMatched(idMatchRequest: IdMatchRequest,
                           count: Int,
                           idMatchResponse: String
                          )(implicit hc: HeaderCarrier): Unit = {

    val auditCount = count + 1

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino
    )

    val response = Json.obj(
      "response" -> idMatchResponse,
      "responseMsg" -> "Matched.",
      "countOfTheAttempt" -> auditCount,
      "isLocked" -> false
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCHED,
      request = request,
      internalId = idMatchRequest.id,
      response = response
    )
  }

  def auditIdentityMatchExceeded(idMatchRequest: IdMatchRequest,
                                 count: Int,
                                 idMatchResponse: String
                                )(implicit hc: HeaderCarrier): Unit = {

    val auditCount = count + 1

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino
    )

    val response = Json.obj(
      "response" -> idMatchResponse,
      "responseMsg" -> "Max attempts exceeded.",
      "countOfTheAttempt" -> auditCount,
      "isLocked" -> true
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT_EXCEEDED,
      request = request,
      internalId = idMatchRequest.id,
      response = response
    )
  }

  def auditIdentityMatchApiError(idMatchRequest: IdMatchRequest,
                                 count: Int,
                                 idMatchResponse: String
                               )(implicit hc: HeaderCarrier): Unit = {

    val auditCount = count + 1

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino
    )

    val response = Json.obj(
      "response" -> idMatchResponse,
      "responseMsg" -> "Identity match api error.",
      "countOfTheAttempt" -> auditCount,
      "isLocked" -> false
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_API_ERROR,
      request = request,
      internalId = idMatchRequest.id,
      response = response
    )
  }

  def auditOutboundCall[T](request: T)(implicit hc: HeaderCarrier, writes: OWrites[T]): Unit =
    auditConnector.sendExplicitAudit[T](TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_OUTBOUND_REQUEST, request)
}

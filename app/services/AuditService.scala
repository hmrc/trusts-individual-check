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

package services

import javax.inject.Inject
import models.IdMatchRequest
import models.auditing.{GetTrustAuditEvent, TrustAuditing}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class AuditService @Inject()(auditConnector: AuditConnector){

  import scala.concurrent.ExecutionContext.Implicits._

  def audit(event: String,
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
                              count: Int
                             )(implicit hc: HeaderCarrier): Unit = {

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino,
      "countOfTheAttempt" -> count,
      "isLocked" -> false
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT,
      request = request,
      internalId = idMatchRequest.id,
      response = Json.obj("response" -> "Match attempt.")
    )
  }

  def auditIdentityMatched(idMatchRequest: IdMatchRequest,
                              count: Int
                             )(implicit hc: HeaderCarrier): Unit = {

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino,
      "countOfTheAttempt" -> count,
      "isLocked" -> false
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCHED,
      request = request,
      internalId = idMatchRequest.id,
      response = Json.obj("response" -> "Matched.")
    )
  }

  def auditIdentityMatchExceeded(idMatchRequest: IdMatchRequest,
                              count: Int
                             )(implicit hc: HeaderCarrier): Unit = {

    val request = Json.obj(
      "forename" -> idMatchRequest.forename,
      "surname" -> idMatchRequest.surname,
      "dateOfBirth" -> idMatchRequest.birthDate,
      "nino" -> idMatchRequest.nino,
      "countOfTheAttempt" -> count,
      "isLocked" -> true
    )

    audit(
      event = TrustAuditing.LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT_EXCEEDED,
      request = request,
      internalId = idMatchRequest.id,
      response = Json.obj("response" -> "Max attempts exceeded.")
    )
  }

}
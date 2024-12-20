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
import models.auditing.GetTrustAuditEvent
import org.mockito.ArgumentMatchers.{any, eq => equalTo}
import org.mockito.Mockito.verify
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.BaseSpec

class AuditServiceSpec extends BaseSpec {

  val sessionId = "XXX1234567890"

  "auditIdentityMatchError" should {

    "send event when matched" in {

      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val idMatchRequest = IdMatchRequest(
        id = "id",
        nino = "NH111111A",
        surname = "surname",
        forename = "forename",
        birthDate = "01/01/1970"
      )

      val request = Json.obj(
        "forename" -> "forename",
        "surname" -> "surname",
        "dateOfBirth" -> "01/01/1970",
        "nino" -> "NH111111A"
      )

      val response = Json.obj(
        "response" -> "Match",
        "responseMsg" -> "Matched.",
        "countOfTheAttempt" -> 2,
        "isLocked" -> false
      )

      service.auditIdentityMatched(
        idMatchRequest,
        1,
        "Match"
      )

      val expectedAuditData = GetTrustAuditEvent(
        request,
        "id",
        response
      )

      verify(connector).sendExplicitAudit[GetTrustAuditEvent](
        equalTo("LeadTrusteeIdentityMatched"),
        equalTo(expectedAuditData))(any(), any(), any())

    }

    "send event when matching attempt failed" in {

      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val idMatchRequest = IdMatchRequest(
        id = "id",
        nino = "NH111111A",
        surname = "surname",
        forename = "forename",
        birthDate = "01/01/1970"
      )

      val request = Json.obj(
        "forename" -> "forename",
        "surname" -> "surname",
        "dateOfBirth" -> "01/01/1970",
        "nino" -> "NH111111A"
      )

      val response = Json.obj(
        "response" -> "NotMatched",
        "responseMsg" -> "Match attempt.",
        "countOfTheAttempt" -> 1,
        "isLocked" -> false
      )

      service.auditIdentityMatchAttempt(
        idMatchRequest,
        0,
        "NotMatched")

      val expectedAuditData = GetTrustAuditEvent(
        request,
        "id",
        response
      )

      verify(connector).sendExplicitAudit[GetTrustAuditEvent](
        equalTo("LeadTrusteeIdentityMatchAttempt"),
        equalTo(expectedAuditData))(any(), any(), any())

    }

    "send event when matching attempts exceeded" in {

      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val idMatchRequest = IdMatchRequest(
        id = "id",
        nino = "NH111111A",
        surname = "surname",
        forename = "forename",
        birthDate = "01/01/1970"
      )

      val request = Json.obj(
        "forename" -> "forename",
        "surname" -> "surname",
        "dateOfBirth" -> "01/01/1970",
        "nino" -> "NH111111A"
      )

      val response = Json.obj(
        "response" -> "NotMatched",
        "responseMsg" -> "Max attempts exceeded.",
        "countOfTheAttempt" -> 5,
        "isLocked" -> true
      )

      service.auditIdentityMatchExceeded(
        idMatchRequest,
        4,
        "NotMatched"
      )

      val expectedAuditData = GetTrustAuditEvent(
        request,
        "id",
        response
      )

      verify(connector).sendExplicitAudit[GetTrustAuditEvent](
        equalTo("LeadTrusteeIdentityMatchAttemptExceeded"),
        equalTo(expectedAuditData))(any(), any(), any())

    }


    "send event when match API error" in {

      val connector = mock[AuditConnector]
      val service = new AuditService(connector)

      val idMatchRequest = IdMatchRequest(
        id = "id",
        nino = "NH111111A",
        surname = "surname",
        forename = "forename",
        birthDate = "01/01/1970"
      )

      val request = Json.obj(
        "forename" -> "forename",
        "surname" -> "surname",
        "dateOfBirth" -> "01/01/1970",
        "nino" -> "NH111111A"
      )

      val response = Json.obj(
        "response" -> "ErrorResponse",
        "responseMsg" -> "Identity match api error.",
        "countOfTheAttempt" -> 2,
        "isLocked" -> false
      )

      service.auditIdentityMatchApiError(
        idMatchRequest,
        1,
        "ErrorResponse")

      val expectedAuditData = GetTrustAuditEvent(
        request,
        "id",
        response
      )

      verify(connector).sendExplicitAudit[GetTrustAuditEvent](
        equalTo("LeadTrusteeIdentityMatchApiError"),
        equalTo(expectedAuditData))(any(), any(), any())
    }
  }
}

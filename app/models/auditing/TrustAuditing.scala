/*
 * Copyright 2023 HM Revenue & Customs
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

package models.auditing

object TrustAuditing {
  val LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT_EXCEEDED = "LeadTrusteeIdentityMatchAttemptExceeded"
  val LEAD_TRUSTEE_IDENTITY_MATCH_ATTEMPT = "LeadTrusteeIdentityMatchAttempt"
  val LEAD_TRUSTEE_IDENTITY_MATCHED = "LeadTrusteeIdentityMatched"
  val LEAD_TRUSTEE_IDENTITY_MATCH_API_ERROR = "LeadTrusteeIdentityMatchApiError"
  val LEAD_TRUSTEE_IDENTITY_MATCH_OUTBOUND_REQUEST = "LeadTrusteeIdentityMatchOutboundRequest"
}

/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers as MustMatchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.http.HttpResponse

class IdMatchApiHttpReadsSpec extends AnyWordSpec with MustMatchers {

  "IdMatchApiHttpReads" should {

    "return DownstreamBadRequest when BAD_REQUEST contains valid failures" in {

      val response = HttpResponse(
        status = BAD_REQUEST,
        body = """
          {
            "failures": {
              "code": "INVALID_NINO",
              "reason": "Invalid National Insurance Number"
            }
          }
        """
      )

      val result = IdMatchApiHttpReads.httpReads.read(
        "POST",
        "/individual-check",
        response
      )

      result mustBe DownstreamBadRequest(
        ErrorResponseDetail(
          code = "INVALID_NINO",
          reason = "Invalid National Insurance Number"
        )
      )
    }

    "return DownstreamServerError when BAD_REQUEST has no failures" in {

      val response = HttpResponse(
        status = BAD_REQUEST,
        body = """{"message": "Bad Request"}"""
      )

      val result = IdMatchApiHttpReads.httpReads.read(
        "POST",
        "/individual-check",
        response
      )

      result mustBe DownstreamServerError
    }

    "return DownstreamServerError when BAD_REQUEST contains invalid failures" in {

      val response = HttpResponse(
        status = BAD_REQUEST,
        body = """
          {
            "failures": {
              "code": "INVALID_NINO"
            }
          }
        """
      )

      val result = IdMatchApiHttpReads.httpReads.read(
        "POST",
        "/individual-check",
        response
      )

      result mustBe DownstreamServerError
    }
  }

}

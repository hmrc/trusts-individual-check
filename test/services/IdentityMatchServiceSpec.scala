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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import exceptions.LimitException
import models.api1585.{IdMatchApiError, NinoNotFound}
import models.{IdMatchResponse, OperationSucceeded}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import util.IdentityMatchHelper

import scala.concurrent.Future

class IdentityMatchServiceSpec extends IdentityMatchHelper with FutureAwaits with DefaultAwaitTimeout {

  val individualsMatchUrl = "/individuals/match"

  lazy val identityMatchService: IdentityMatchService = application.injector.instanceOf[IdentityMatchService]

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockIndividualCheckRepository)

    when(mockIndividualCheckRepository.incrementCounter(any())).thenReturn(Future.successful(OperationSucceeded))
    when(mockIndividualCheckRepository.getCounter(any())).thenReturn(Future.successful(0))
    when(mockIndividualCheckRepository.clearCounter(any())).thenReturn(Future.successful(OperationSucceeded))
  }

  "Identity Match Connector" should {

    "parse response correctly" when {

      "success is returned" in {
        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchSuccess.toString())
          )
        )

        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(successRequest)),
          matched = true
        )

        verify(mockAuditService, times(1)).auditOutboundCall(any())(any(), any())
        verify(mockAuditService).auditIdentityMatched(any(), any(), mockEq("Match"))(any())
      }

      "nino is not found" in {
        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(matchError.toString())
          )
        )

        shouldRespondWithSpecifiedError(
          response = await(identityMatchService.matchId(notFoundRequest)),
          error = NinoNotFound
        )

        verify(mockAuditService, times(1)).auditOutboundCall(any())(any(), any())
        verify(mockAuditService).auditIdentityMatchApiError(any(), any(), any())(any())
      }

      "maximum number of attempts is reached" in {

        when(mockIndividualCheckRepository.getCounter(any())).thenReturn(Future.successful(5))

        intercept[LimitException] {
          await(identityMatchService.matchId(maxAttemptsRequest))

          verify(mockAuditService).auditIdentityMatchExceeded(any(), any(), any())(any())
        }
      }
    }

    "increment counter" when {

      "not matched" in {

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchFailure.toString())
          )
        )

        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(failureRequest)),
          matched = false
        )

        verify(mockAuditService, times(1)).auditOutboundCall(any())(any(), any())
        verify(mockAuditService).auditIdentityMatchAttempt(any(), any(), mockEq("NotMatched"))(any())
        verify(mockIndividualCheckRepository, times(1)).incrementCounter(mockEq(idString))
      }

      "nino not found" in {

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody(matchFailure.toString())
          )
        )

        shouldRespondWithSpecifiedError(
          response = await(identityMatchService.matchId(notFoundRequest)),
          error = NinoNotFound
        )

        verify(mockAuditService, times(1)).auditOutboundCall(any())(any(), any())
        verify(mockAuditService).auditIdentityMatchApiError(any(), any(), any())(any())
        verify(mockIndividualCheckRepository, times(1)).incrementCounter(mockEq(idString))
      }


      "reset the counter on success" in {

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchFailure.toString())
          )
        )

        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(failureRequest)),
          matched = false
        )

        verify(mockIndividualCheckRepository, times(1)).incrementCounter(mockEq(idString))

        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(failureRequest)),
          matched = false
        )
        verify(mockIndividualCheckRepository, times(2)).incrementCounter(mockEq(idString))

        wireMockServer.stubFor(
          post(urlEqualTo(individualsMatchUrl)).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(matchSuccess.toString())
          )
        )

        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(successRequest)),
          matched = true
        )

        verify(mockIndividualCheckRepository, times(1)).clearCounter(mockEq(idString))
        verify(mockAuditService, times(3)).auditOutboundCall(any())(any(), any())
      }

    }
  }


  def shouldRespondWithSpecifiedMatch(response: Either[IdMatchApiError, IdMatchResponse], matched: Boolean): Unit = {
    response match {
      case Left(error) => fail(s"Should not return errors: $error")
      case Right(response) =>
        response.id mustBe idString
        response.idMatch mustBe matched
    }
  }

  def shouldRespondWithSpecifiedError(response: Either[IdMatchApiError, IdMatchResponse], error: IdMatchApiError): Unit = {
    response match {
      case Right(_) => fail("Should return errors")
      case Left(errors) => errors mustBe error
    }
  }

}


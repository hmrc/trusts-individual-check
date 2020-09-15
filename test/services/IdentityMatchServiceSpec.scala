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

package services

import connectors.IdentityMatchConnector
import models.{IdMatchError, IdMatchResponse}
import org.mockito.ArgumentMatchers.{eq => mockEq}
import org.mockito.Mockito.{times, verify}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import util.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global

class IdentityMatchServiceSpec extends BaseSpec with FutureAwaits with DefaultAwaitTimeout {

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  val identityMatchConnector = application.injector.instanceOf[IdentityMatchConnector]
  val identityMatchService = application.injector.instanceOf[IdentityMatchService]

  "Identity Match Connector" should {

    "parse response correctly" when {

      "success is returned" in {
        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(successRequest)),
          matched = true)
      }

      "error is returned" in {
        shouldRespondWithSpecifiedError(
          response = await(identityMatchService.matchId(errorRequest)),
          error = "Something went wrong")
      }

      "maximum number of attempts is reached" in {
        shouldRespondWithSpecifiedError(
          response = await(identityMatchService.matchId(maxAttemptsRequest)),
          error = "Individual check - retry limit reached (3)")
      }
    }

    "reset the counter on success" in {

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(failureRequest)),
        matched = false)

      verify(repository, times(1)).incrementCounter(mockEq(idString))

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(failureRequest)),
        matched = false)

      verify(repository, times(2)).incrementCounter(mockEq(idString))

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(successRequest)),
        matched = true)

      verify(repository, times(1)).clearCounter(mockEq(idString))
    }
  }

  def shouldRespondWithSpecifiedMatch(response: Either[IdMatchError, IdMatchResponse], matched: Boolean):Unit = {
    response match {
      case Left(_) => fail("Should not return errors")
      case Right(response) =>
        response.id mustBe idString
        response.idMatch mustBe matched
    }
  }

  def shouldRespondWithSpecifiedError(response: Either[IdMatchError, IdMatchResponse], error: String):Unit = {
    response match {
      case Right(_) => fail("Should return errors")
      case Left(errors) => errors.errors.contains(error) mustBe true
    }
  }
}

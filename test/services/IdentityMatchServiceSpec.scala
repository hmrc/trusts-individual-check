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
import exceptions.LimitException
import models.IdMatchResponse
import models.api1585.{IdMatchApiError, NinoNotFound}
import org.mockito.ArgumentMatchers.{eq => mockEq}
import org.mockito.Mockito.{times, verify}
import play.api.inject.bind
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import util.{BaseSpec, IdentityMatchHelper}

import scala.concurrent.ExecutionContext.Implicits.global

class IdentityMatchServiceSpec extends BaseSpec with IdentityMatchHelper with FutureAwaits with DefaultAwaitTimeout {

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  override lazy val application = applicationBuilder()
    .overrides(bind[HttpClient].toInstance(httpClient))
    .overrides(bind[IndividualCheckRepository].toInstance(mockIndividualCheckRepository))
    .build()

  val identityMatchConnector = application.injector.instanceOf[IdentityMatchConnector]
  val identityMatchService = application.injector.instanceOf[IdentityMatchService]

  "Identity Match Connector" should {

    "parse response correctly" when {

      "success is returned" in {
        shouldRespondWithSpecifiedMatch(
          response = await(identityMatchService.matchId(successRequest)),
          matched = true)
      }

      "nino is not found" in {
        shouldRespondWithSpecifiedError(
          response = await(identityMatchService.matchId(notFoundRequest)),
          error = NinoNotFound)
      }

      "maximum number of attempts is reached" in {
        intercept[LimitException]{
          await(identityMatchService.matchId(maxAttemptsRequest))
        }
      }
    }

    "reset the counter on success" in {

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(failureRequest)),
        matched = false)

      verify(mockIndividualCheckRepository, times(1)).incrementCounter(mockEq(idString))

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(failureRequest)),
        matched = false)

      verify(mockIndividualCheckRepository, times(2)).incrementCounter(mockEq(idString))

      shouldRespondWithSpecifiedMatch(
        response = await(identityMatchService.matchId(successRequest)),
        matched = true)

      verify(mockIndividualCheckRepository, times(1)).clearCounter(mockEq(idString))
    }
  }

  def shouldRespondWithSpecifiedMatch(response: Either[IdMatchApiError, IdMatchResponse], matched: Boolean):Unit = {
    response match {
      case Left(error) => fail(s"Should not return errors: ${error}")
      case Right(response) =>
        response.id mustBe idString
        response.idMatch mustBe matched
    }
  }

  def shouldRespondWithSpecifiedError(response: Either[IdMatchApiError, IdMatchResponse], error: IdMatchApiError):Unit = {
    response match {
      case Right(_) => fail("Should return errors")
      case Left(errors) => errors mustBe error
    }
  }
}

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

import config.AppConfig
import connectors.IdentityMatchConnector
import models.{IdMatchError, IdMatchResponse}
import org.mockito.ArgumentMatchers.{eq => mockEq}
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.IdentityMatchHelper

import scala.concurrent.ExecutionContext.Implicits.global

class IdentityMatchServiceSpec extends AnyWordSpec  with IdentityMatchHelper
                                                    with Matchers
                                                    with GuiceOneAppPerSuite
                                                    with FutureAwaits
                                                    with DefaultAwaitTimeout {

  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig     = new AppConfig(configuration, serviceConfig)

  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  val identityMatchConnector = new IdentityMatchConnector(httpClient, appConfig)
  val identityMatchService = new IdentityMatchService(identityMatchConnector, repository, appConfig)

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

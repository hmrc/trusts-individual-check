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
import exceptions.LimitException
import javax.inject.Inject
import models.{IdMatchApiResponseSuccess, IdMatchResponse}
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class IdentityMatchService @Inject()(val connector: IdentityMatchConnector,
                                     val repository: IndividualCheckRepository,
                                     val appConfig: AppConfig) {


  def matchId(id: String, nino: String, surname: String, forename: String, birthDate: String)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[IdMatchResponse] = {
    repository.getCounter(id).flatMap { count =>
      if(count >= appConfig.maxIdAttempts) {
        throw new LimitException(s"Individual check - retry limit reached (${appConfig.maxIdAttempts})")
      } else {
        Try {
          perform(id, nino, surname, forename, birthDate)
        } match {
          case Success(value) => value
          case Failure(_) => throw new Exception("Something went wrong")
        }
      }
    }
  }

  private def perform(id: String, nino: String, surname: String, forename: String, birthDate: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IdMatchResponse] = {
    connector.matchId(nino, surname, forename, birthDate).map {
      case Right(IdMatchApiResponseSuccess(matched)) =>
        if(matched) {
          repository.clearCounter(id)
        } else {
          repository.incrementCounter(id)
        }
        IdMatchResponse(id = id, idMatch = matched)
      case Left(x) =>
        throw new Exception("Something went wrong")
    }
  }
}

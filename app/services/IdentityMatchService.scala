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
import exceptions.{InvalidIdMatchRequest, InvalidIdMatchResponse, LimitException}
import javax.inject.Inject
import models.{BinaryResult, IdMatchError, IdMatchRequest, IdMatchResponse}
import models.api1585.IdMatchApiResponseSuccess
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class IdentityMatchService @Inject()(val connector: IdentityMatchConnector,
                                     val repository: IndividualCheckRepository,
                                     val appConfig: AppConfig) {


  def matchId(request: IdMatchRequest)(implicit ec: ExecutionContext): Future[Either[IdMatchError, IdMatchResponse]] = {

    limitedMatch(request).recoverWith {
      case e: InvalidIdMatchResponse => getErrorResponse(s"Something went wrong: $e")
      case e: InvalidIdMatchRequest => getErrorResponse(s"Something went wrong: $e")
    }
  }

  def clearCounter(id: String): Future[BinaryResult] =
    repository.clearCounter(id)

  private def limitedMatch(request: IdMatchRequest)(implicit ec: ExecutionContext): Future[Either[IdMatchError, IdMatchResponse]] = {

    repository.getCounter(request.id).flatMap { count =>
      if (count >= appConfig.maxIdAttempts) {
        throw new LimitException(s"Individual check - retry limit reached (${appConfig.maxIdAttempts})")
      } else {
        connector.matchId(request.nino, request.surname, request.forename, request.birthDate).map {
          case Right(IdMatchApiResponseSuccess(matched)) =>
            if(matched) {
              clearCounter(request.id)
            } else {
              repository.incrementCounter(request.id)
            }
            Right(IdMatchResponse(id = request.id, idMatch = matched))
          case Left(_) =>
            Left(IdMatchError(Seq("Something went wrong")))
        }
      }
    }
  }

  private def getErrorResponse(msg: String) = {
    Future.successful(Left(IdMatchError(Seq(msg))))
  }
}

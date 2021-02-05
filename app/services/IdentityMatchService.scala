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

import config.AppConfig
import connectors.IdentityMatchConnector
import exceptions.{InvalidIdMatchRequest, LimitException}
import javax.inject.Inject
import models.api1585.{DownstreamServerError, IdMatchApiError, IdMatchApiResponseSuccess}
import models.{BinaryResult, IdMatchRequest, IdMatchResponse}
import play.api.Logging
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import scala.concurrent.{ExecutionContext, Future}

class IdentityMatchService @Inject()(val connector: IdentityMatchConnector,
                                     val repository: IndividualCheckRepository,
                                     auditService: AuditService,
                                     val appConfig: AppConfig) extends Logging {

  def matchId(request: IdMatchRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[IdMatchApiError, IdMatchResponse]] = {

    limitedMatch(request).recoverWith {
      case _: InvalidIdMatchRequest =>
        logger.warn(s"[Session ID: ${Session.id(hc)}] unable to send request, failed validation")
        Future.successful(Left(DownstreamServerError))
    }
  }

  def clearCounter(id: String)(implicit hc: HeaderCarrier): Future[BinaryResult] = {
    logger.info(s"[Session ID: ${Session.id(hc)}] Lock cleared")
    repository.clearCounter(id)
  }


  private def limitedMatch(request: IdMatchRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[IdMatchApiError, IdMatchResponse]] = {

    repository.getCounter(request.id).flatMap { count =>
      if (count >= appConfig.maxIdAttempts) {
        logger.info(s"[Session ID: ${Session.id(hc)}] max attempts exceeded. Current count: $count")
        auditService.auditIdentityMatchExceeded(
          idMatchRequest = request,
          count = count,
          idMatchResponse = "NotMatched"
        )
        throw new LimitException(s"Individual check - retry limit reached (${appConfig.maxIdAttempts})")
      } else {
        connector.matchId(request.nino, request.surname, request.forename, request.birthDate).map {
          case IdMatchApiResponseSuccess(matched) =>
            if(matched) {
              logger.info(s"[Session ID: ${Session.id(hc)}] Matched")
              auditService.auditIdentityMatched(
                idMatchRequest = request,
                count = count,
                idMatchResponse = "Match"
              )
              clearCounter(request.id)
            } else {
              logger.info(s"[Session ID: ${Session.id(hc)}] Not matched, increasing counter")
              auditService.auditIdentityMatchAttempt(
                idMatchRequest = request,
                count = count,
                idMatchResponse = "NotMatched"
              )
              repository.incrementCounter(request.id)
            }
            Right(IdMatchResponse(id = request.id, idMatch = matched))
          case errorResponse: IdMatchApiError =>
            auditService.auditIdentityMatchApiError(
              idMatchRequest = request,
              count = count,
              idMatchResponse = errorResponse.toString
            )
            Left(errorResponse)
        }
      }
    }
  }
}

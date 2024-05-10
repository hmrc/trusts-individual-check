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

import config.AppConfig
import connectors.IdentityMatchConnector
import exceptions.{InvalidIdMatchRequest, LimitException}
import models.api1585.{DownstreamServerError, IdMatchApiError, IdMatchApiResponseSuccess, NinoNotFound}
import models.{BinaryResult, IdMatchRequest, IdMatchResponse, OperationSucceeded}
import play.api.Logging
import repositories.IndividualCheckRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Session

import javax.inject.Inject
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

  def getCounter(id: String): Future[Int] = {
    repository.getCounter(id)
  }

  private def limitedMatch(request: IdMatchRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[IdMatchApiError, IdMatchResponse]] = {

    getCounter(request.id) flatMap { count =>
      if (count >= appConfig.maxIdAttempts) {
        logger.info(s"[Session ID: ${Session.id(hc)}] max attempts exceeded. Current count: $count")
        auditService.auditIdentityMatchExceeded(request, count, idMatchResponse = "NotMatched")
        throw new LimitException(s"Individual check - retry limit reached (${appConfig.maxIdAttempts})")
      } else {
        connector.matchId(request.nino, request.surname, request.forename, request.birthDate) flatMap {
          case IdMatchApiResponseSuccess(matched) =>
            auditResultAndUpdateCounter(matched, request, count) map { _ =>
              Right(IdMatchResponse(id = request.id, idMatch = matched))
            }
          case errorResponse: IdMatchApiError =>
            auditErrorAndUpdateCounter(errorResponse, request, count) map { _ =>
              Left(errorResponse)
            }
        }
      }
    }
  }

  private def auditResultAndUpdateCounter(matched: Boolean, request: IdMatchRequest, count: Int)
                                         (implicit hc: HeaderCarrier): Future[BinaryResult] = {
    if (matched) {
      logger.info(s"[Session ID: ${Session.id(hc)}] Matched. Resetting counter.")
      auditService.auditIdentityMatched(request, count, "Match")
      clearCounter(request.id)
    } else {
      logger.info(s"[Session ID: ${Session.id(hc)}] Not matched. Increasing counter.")
      auditService.auditIdentityMatchAttempt(request, count, "NotMatched")
      repository.incrementCounter(request.id)
    }
  }

  private def auditErrorAndUpdateCounter(errorResponse: IdMatchApiError, request: IdMatchRequest, count: Int)
                                        (implicit hc: HeaderCarrier): Future[BinaryResult] = {

    auditService.auditIdentityMatchApiError(request, count, errorResponse.toString)

    errorResponse match {
      case NinoNotFound =>
        logger.info(s"[Session ID: ${Session.id(hc)}] NINO not found. Increasing counter.")
        repository.incrementCounter(request.id)
      case _ =>
        Future.successful(OperationSucceeded)
    }
  }
}

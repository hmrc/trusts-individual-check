/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import controllers.actions.IdentifierAction
import exceptions.{InvalidIdMatchRequest, LimitException}
import models.api1585._
import models.{IdMatchError, IdMatchRequest, IdMatchResponse}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import services.IdentityMatchService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Session

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class IndividualCheckController @Inject()(service: IdentityMatchService,
                                          cc: ControllerComponents,
                                          identify: IdentifierAction
                                         )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def individualCheck(): Action[JsValue] = identify.async(parse.json) {
    implicit request =>
      Future(validateRequest(request)) flatMap { r =>
        service.matchId(r)
      } map processResponse recoverWith {
          case e: LimitException => Future.successful(Forbidden(getError(e.getLocalizedMessage)))
          case e: InvalidIdMatchRequest => Future.successful(BadRequest(getError(e.getLocalizedMessage)))
      }
  }

  private def validateRequest(request: Request[JsValue])(implicit hc: HeaderCarrier): IdMatchRequest = {
    request.body.validate[IdMatchRequest] match {
      case JsSuccess(idRequest, _) => idRequest
      case JsError(errors) =>
        logger.error(s"[Session ID: ${Session.id(hc)}][Unable to validate payload due to ${JsError.toJson(errors)}]")
        throw new InvalidIdMatchRequest(s"Could not validate the request")
    }
  }

  private def processResponse(response: Either[IdMatchApiError, IdMatchResponse]): Result = {

    response match {
      case Left(DownstreamServerError) =>
        val response = IdMatchError(Seq("IF is currently experiencing problems that require live service intervention"))
        InternalServerError(Json.toJson(response))
      case Left(DownstreamBadRequest(reason)) =>
        val response = IdMatchError(Seq(reason.reason))
        InternalServerError(Json.toJson(response))
      case Left(NinoNotFound) =>
        val response = IdMatchError(Seq("Dependent service indicated that no data can be found"))
        NotFound(Json.toJson(response))
      case Left(DownstreamServiceUnavailable) =>
        val response = IdMatchError(Seq("Dependent service is unavailable"))
        ServiceUnavailable(Json.toJson(response))
      case Right(value) =>
        Ok(Json.toJson(value))
    }
  }

  private def getError(msg: String): JsValue = {
    Json.toJson(IdMatchError(Seq(msg)))
  }

  def failedAttempts(id: String): Action[AnyContent] = identify.async {
    service.getCounter(id) map { count =>
      Ok(Json.toJson(count))
    }
  }
}

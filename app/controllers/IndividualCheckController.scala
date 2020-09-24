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

package controllers

import controllers.actions.IdentifierAction
import exceptions.{InvalidIdMatchRequest, LimitException}
import javax.inject.{Inject, Singleton}
import models.api1585.ErrorResponseDetail
import models.{IdMatchError, IdMatchRequest, IdMatchResponse, IdMatchStringError}
import play.api.libs.json.{JsArray, JsError, JsString, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import services.IdentityMatchService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class IndividualCheckController @Inject()(service: IdentityMatchService,
                                          cc: ControllerComponents,
                                          identify: IdentifierAction
                                         )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def individualCheck(): Action[JsValue] = identify.async(parse.json) {
    implicit request => {
      Future { validateRequest(request) } flatMap (r => service.matchId(r)) map processResponse recoverWith {
        case e: LimitException => Future.successful(Forbidden(getError(e.getLocalizedMessage)))
        case e: InvalidIdMatchRequest => Future.successful(BadRequest(getError(e.getLocalizedMessage)))
      }
    }
  }

  private def validateRequest(request: Request[JsValue]): IdMatchRequest = {
    request.body.validate[IdMatchRequest] match {
      case JsSuccess(idRequest, _) => idRequest
      case JsError(_) => throw new InvalidIdMatchRequest("Could not validate the request")
    }
  }

  private def processResponse(response: Either[IdMatchError, IdMatchResponse]): Result = {

    response match {
      case Left(IdMatchError(Seq(ErrorResponseDetail("INTERNAL_SERVER_ERROR", reason)))) =>
        InternalServerError(Json.obj("errors" -> Seq(JsString(reason))))
      case Right(value) =>
        Ok(Json.toJson(value))
    }
  }

  private def getError(msg: String): JsValue = {
    Json.toJson(IdMatchStringError(Seq(msg)))
  }
}

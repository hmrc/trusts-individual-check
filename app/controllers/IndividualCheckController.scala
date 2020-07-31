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

import config.AppConfig
import exceptions.InvalidIdMatchRequest
import javax.inject.{Inject, Singleton}
import models.{IdMatchError, IdMatchRequest, IdMatchResponse}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import services.IdentityMatchService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class IndividualCheckController @Inject()(appConfig: AppConfig, service: IdentityMatchService, cc: ControllerComponents)
                                         (implicit ec: ExecutionContext) extends BackendController(cc) {

  // TODO: Implement Auth?

  def individualCheck(): Action[JsValue] = Action.async(parse.json) {
    implicit request => {
      Future { validateRequest(request) } flatMap (r => service.matchId(r)) map processResponse recoverWith {
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
    Ok(response.fold(e => Json.toJson(e), x => Json.toJson(x)))
  }

  private def getError(msg: String): JsValue = {
    Json.toJson(IdMatchError(Seq(msg)))
  }
}

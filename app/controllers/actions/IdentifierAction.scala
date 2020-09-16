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

package controllers.actions

import com.google.inject.Inject
import models.{ErrorResponse, IdentifierRequest}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                              val parser: BodyParsers.Default)
                                             (implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions {

  private val logger = Logger("IdentifierAction")

  def invokeBlock[A](request: Request[A],
                     block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    val retrievals = Retrievals.internalId and
                     Retrievals.affinityGroup

    implicit val hc : HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    authorised().retrieve(retrievals) {
      case Some(internalId) ~ Some(Agent) =>
        block(IdentifierRequest(request, internalId, Agent))
      case Some(internalId) ~ Some(Organisation) =>
        block(IdentifierRequest(request, internalId, Organisation))
      case _ =>
        logger.info(s"[IdentifierAction] Insufficient enrolment")
        Future.successful(Unauthorized(Json.toJson(ErrorResponse("UNAUTHORISED", "Insufficient enrolment for authorised user."))))
    } recoverWith {
      case e : AuthorisationException =>
        logger.info(s"[IdentifierAction] AuthorisationException: $e")
        Future.successful(Unauthorized)
    }
  }

}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]

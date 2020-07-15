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

package repositories

import javax.inject.Inject
import models.IndividualCheckCount
import play.api.libs.json.{Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.LastError
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class IndividualCheckRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  private val documentExistsErrorCodeValue = 11000
  private lazy val documentExistsErrorCode = Some(documentExistsErrorCodeValue)

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](IndividualCheckRepository.collectionName))

  def fetch(credId: String): Future[IndividualCheckCount] = {
    collection.flatMap {
      _.find(Json.obj("_id" -> Json.toJson(credId)))
        .one[IndividualCheckCount]
        .map(_.getOrElse(0))
    }
  }

  def insert(checkCount: IndividualCheckCount): Future[Boolean] = {

    val checkCountWithId = Json.toJsObject(checkCount) ++ Json.obj("_id" -> checkCount.credId)

    collection.flatMap {
      _.insert(ordered = false)
        .one(checkCountWithId)
        .map(_ => true)
    } recover {
      case e: LastError if e.code == documentExistsErrorCode =>
        false
    }
  }
}

object IndividualCheckRepository {
  val collectionName = "individual-check-count"
}

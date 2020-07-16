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
import models.{BinaryResult, IndividualCheckCount, OperationSucceeded}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class IndividualCheckRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  val collectionName     : String = "individual-check-counters"

  private def collection : Future[JSONCollection] = mongo.database.map(
    _.collection[JSONCollection](collectionName)
  )

  def getCounter(id: String): Future[Long] = {
    val selector = Json.obj("_id" -> Json.toJson(id))

    collection.flatMap {
      _.find[JsObject, JsObject](selector)
        .one[IndividualCheckCount]
        .map(_.map(_.attempts).getOrElse(0))
    }
  }

  def setCounter(id: String, attempts: Long): Future[BinaryResult] = {

    val selector = Json.obj("_id" -> Json.toJson(id))
    val modifier = Json.obj("$set" -> Json.obj("attempts" -> attempts))

    collection.flatMap {
      _.findAndUpdate[JsObject, JsObject](selector, modifier, upsert = true)
        .map(_ => OperationSucceeded)
    }
  }
}

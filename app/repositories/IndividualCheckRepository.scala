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

package repositories

import javax.inject.Inject
import models.{BinaryResult, IndividualCheckCount, OperationSucceeded}
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.WriteConcern
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

// Tested in integration testing
// $COVERAGE-OFF$
class IndividualCheckRepository @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {

  val collectionName     : String = "individual-check-counters"

  private def collection : Future[JSONCollection] = for {
      _ <- ensureIndexes
      res <- mongo.database.map(_.collection[JSONCollection](collectionName))
    } yield res

  private lazy val idIndex = MongoIndex("id", "id-index", unique = true)

  private lazy val ensureIndexes = {
    for {
      collection              <- mongo.database.map(_.collection[JSONCollection](collectionName))
      createdIdIndex          <- collection.indexesManager.ensure(idIndex)
    } yield createdIdIndex
  }

  def getCounter(id: String): Future[Int] = {
    val selector = Json.obj("id" -> Json.toJson(id))

    collection.flatMap {
      _.find[JsObject, JsObject](selector)
        .one[IndividualCheckCount]
        .map(_.map(_.attempts).getOrElse(0))
    }
  }

  def clearCounter(id: String): Future[BinaryResult] = {
    val selector = Json.obj("id" -> Json.toJson(id))
    collection.flatMap {
      _.findAndRemove(
        selector = selector,
        sort = None,
        fields = None,
        writeConcern = WriteConcern.Default,
        maxTime = None,
        collation = None,
        arrayFilters = Nil).map(_ => OperationSucceeded)
    }
  }

  def incrementCounter(id: String): Future[BinaryResult] = {
    getCounter(id).flatMap(counter => setCounter(id, counter + 1))
  }

  def setCounter(id: String, attempts: Int): Future[BinaryResult] = {

    val selector = Json.obj("id" -> Json.toJson(id))
    val modifier = Json.obj("$set" -> Json.obj("attempts" -> attempts))

    collection.flatMap {
      _.findAndUpdate[JsObject, JsObject](
        selector = selector,
        update = modifier,
        fetchNewObject = true,
        upsert = true,
        sort = None,
        fields = None,
        bypassDocumentValidation = false,
        writeConcern = WriteConcern.Default,
        maxTime = None,
        collation = None,
        arrayFilters = Nil)
        .map(_ => OperationSucceeded)
    }
  }
}

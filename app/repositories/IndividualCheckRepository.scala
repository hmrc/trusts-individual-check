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

package repositories

import javax.inject.Inject
import models.{BinaryResult, IndividualCheckCount, MongoDateTimeFormats, OperationFailed, OperationSucceeded}
import play.api.libs.json.Json
import org.mongodb.scala.model._

import java.util.concurrent.TimeUnit
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

// Tested in integration testing
// $COVERAGE-OFF$
class IndividualCheckRepository @Inject()(mongo: MongoComponent, config: Configuration)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[IndividualCheckCount] (
    collectionName = "individual-check-counters",
    mongoComponent = mongo,
    domainFormat = IndividualCheckCount.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("last-updated-index")
          .expireAfter(config.get[Int]("mongodb.ttl"), TimeUnit.SECONDS)
          .unique(false)
      ),
      IndexModel(
        Indexes.ascending("id"),
        IndexOptions()
          .name("id-index")
          .unique(true)
      )
    )
  ){

  def getCounter(id: String): Future[Int] = {
    val selector = Filters.equal("id", id)
    val res = collection.find(selector).headOption()

    res.map {
      case Some(value) => value.attempts
      case None => 0
    }
  }

  def clearCounter(id: String): Future[BinaryResult] = {
    val res = collection.findOneAndDelete( Filters.equal("id", id)).toFutureOption()
    binaryOutcome(res)
  }

  def incrementCounter(id: String): Future[BinaryResult] = {
    getCounter(id).flatMap(counter => setCounter(id, counter + 1))
  }

  def setCounter(id: String, attempts: Int): Future[BinaryResult] = {
    val selector = Filters.equal("id", id)
    val currentTime = Json.toJson(LocalDateTime.now)(MongoDateTimeFormats.localDateTimeWrite)
    val modifier = Updates.combine(Updates.set("attempts", Codecs.toBson(attempts)), Updates.set("lastUpdated", Codecs.toBson(currentTime)))
    val updateOptions = new FindOneAndUpdateOptions().upsert(true)

    val res = collection.findOneAndUpdate(selector, modifier, updateOptions).toFutureOption()

    binaryOutcome(res)
  }

  def binaryOutcome(result: Future[Option[IndividualCheckCount]]): Future[BinaryResult] = {
    result.flatMap {
      case Some(_) => Future.successful(OperationSucceeded)
      case None => Future.successful(OperationFailed)
    }
  }
}

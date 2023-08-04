/*
 * Copyright 2023 HM Revenue & Customs
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

import models.IndividualCheckCount
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.InsertManyOptions
import org.scalatest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.{Matchers => MustMatchers}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import suite.MongoSuite

class IndividualCheckRepositorySpec extends AnyFreeSpec with MustMatchers with MongoSuite with BeforeAndAfterEach {

  private val repository = createApplication.injector.instanceOf[IndividualCheckRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  "getCounter" - {

    "must give the value for the chosen id, or 0 if a record does not exist" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1),
        IndividualCheckCount("id2", 2)
      )

      val insertOptions = new InsertManyOptions().ordered(false)
      await(repository.collection.insertMany(existingRecords, insertOptions).toFuture())

      await(repository.getCounter("id1")) mustEqual 1
      await(repository.getCounter("id2")) mustEqual 2
      await(repository.getCounter("id3")) mustEqual 0
    }
  }

  "setCounter" - {

    "must set the value for an id, and create a record if one does not exist for that id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1),
        IndividualCheckCount("id2", 2)
      )

      val insertOptions = new InsertManyOptions().ordered(false)
      await(repository.collection.insertMany(existingRecords, insertOptions).toFuture())

      await(repository.setCounter("id1", 2))
      await(repository.setCounter("id2", 3))
      await(repository.setCounter("id3", 1))

      await(repository.getCounter("id1")) mustEqual 2
      await(repository.getCounter("id2")) mustEqual 3
      await(repository.getCounter("id3")) mustEqual 1
    }
  }

  "incrementCounter" - {

    "must increase the existing value for an id by one, or set it to one if a value does not exist for that id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1)
      )

      val insertOptions = new InsertManyOptions().ordered(false)
      await(repository.collection.insertMany(existingRecords, insertOptions).toFuture())

      await(repository.incrementCounter("id1"))
      await(repository.getCounter("id1")) mustEqual 2


      await(repository.incrementCounter("id2"))
      await(repository.getCounter("id2")) mustEqual 1
    }
  }

  "clearCounter" - {

    "must remove the record for an id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1)
      )

      val insertOptions = new InsertManyOptions().ordered(false)
      await(repository.collection.insertMany(existingRecords, insertOptions).toFuture())

      await(repository.clearCounter("id1"))
      await(repository.getCounter("id1")) mustEqual 0
    }
  }
}

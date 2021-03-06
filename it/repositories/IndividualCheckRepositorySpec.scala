package repositories

import models.IndividualCheckCount
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers.running
import suite.MongoSuite
import org.scalatest.matchers.must.{Matchers => MustMatchers}

import scala.concurrent.ExecutionContext.Implicits.global

class IndividualCheckRepositorySpec
  extends AnyFreeSpec
    with MustMatchers
    with ScalaFutures
    with OptionValues
    with MongoSuite
    with IntegrationPatience {


  "getCounter" - {

    "must give the value for the chosen id, or 0 if a record does not exist" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1),
        IndividualCheckCount("id2", 2)
      )

      database.flatMap(_.drop()).futureValue

      database.flatMap {
        _.collection[JSONCollection]("individual-check-counters")
          .insert(ordered = false)
          .many(existingRecords)
      }.futureValue

      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val repo = app.injector.instanceOf[IndividualCheckRepository]

        repo.getCounter("id1").futureValue mustEqual 1
        repo.getCounter("id2").futureValue mustEqual 2
        repo.getCounter("id3").futureValue mustEqual 0
      }
    }
  }

  "setCounter" - {

    "must set the value for an id, and create a record if one does not exist for that id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1),
        IndividualCheckCount("id2", 2)
      )

      database.flatMap(_.drop()).futureValue

      database.flatMap {
        _.collection[JSONCollection]("individual-check-counters")
          .insert(ordered = false)
          .many(existingRecords)
      }.futureValue

      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val repo = app.injector.instanceOf[IndividualCheckRepository]

        repo.setCounter("id1", 2).futureValue
        repo.setCounter("id2", 3).futureValue
        repo.setCounter("id3", 1).futureValue

        repo.getCounter("id1").futureValue mustEqual 2
        repo.getCounter("id2").futureValue mustEqual 3
        repo.getCounter("id3").futureValue mustEqual 1
      }
    }
  }

  "incrementCounter" - {

    "must increase the existing value for an id by one, or set it to one if a value does not exist for that id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1)
      )

      database.flatMap(_.drop()).futureValue

      database.flatMap {
        _.collection[JSONCollection]("individual-check-counters")
          .insert(ordered = false)
          .many(existingRecords)
      }.futureValue

      val app = new GuiceApplicationBuilder().build()

      running(app) {
        val repo = app.injector.instanceOf[IndividualCheckRepository]
        repo.incrementCounter("id1").futureValue
        repo.getCounter("id1").futureValue mustEqual 2


        repo.incrementCounter("id2").futureValue
        repo.getCounter("id2").futureValue mustEqual 1
      }
    }
  }

  "clearCounter" - {

    "must remove the record for an id" in {

      val existingRecords = List(
        IndividualCheckCount("id1", 1)
      )

      database.flatMap(_.drop()).futureValue

      database.flatMap {
        _.collection[JSONCollection]("individual-check-counters")
          .insert(ordered = false)
          .many(existingRecords)
      }.futureValue

      val app = new GuiceApplicationBuilder().build()

      running(app) {
        val repo = app.injector.instanceOf[IndividualCheckRepository]
        repo.clearCounter("id1").futureValue
        repo.getCounter("id1").futureValue mustEqual 0
      }
    }
  }
}

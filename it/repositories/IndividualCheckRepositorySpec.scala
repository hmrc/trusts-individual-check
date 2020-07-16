package repositories

import models.IndividualCheckCount
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.{MustMatchers, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.play.json.collection.JSONCollection
import play.api.test.Helpers.running
import suite.MongoSuite

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
        IndividualCheckCount("CredId1", 1),
        IndividualCheckCount("CredId2", 2)
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

        repo.getCounter("CredId1").futureValue mustEqual 1L
        repo.getCounter("CredId2").futureValue mustEqual 2L
        repo.getCounter("CredId3").futureValue mustEqual 0L
      }
    }
  }

  "setCounter" - {

    "must set the value for a credId, and create a record if one does not exist for that id" in {

      val existingRecords = List(
        IndividualCheckCount("CredId1", 1),
        IndividualCheckCount("CredId2", 2)
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

        repo.setCounter("CredId1", 2L).futureValue
        repo.setCounter("CredId2", 3L).futureValue
        repo.setCounter("CredId3", 1L).futureValue

        repo.getCounter("CredId1").futureValue mustEqual 2L
        repo.getCounter("CredId2").futureValue mustEqual 3L
        repo.getCounter("CredId3").futureValue mustEqual 1L
      }
    }
  }
}

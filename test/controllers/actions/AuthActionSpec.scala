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

import akka.stream.Materializer
import com.google.inject.Inject
import config.AppConfig
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits._

class AuthActionSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  private val cc = stubControllerComponents()

  implicit lazy val mtrlzr = app.injector.instanceOf[Materializer]

  private val appConfig = app.injector.instanceOf[AppConfig]

  def fakeRequest : FakeRequest[JsValue] = FakeRequest("POST", "")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody(Json.parse("{}"))

  class Harness(authAction: IdentifierAction) {
    def onSubmit() = authAction.apply(BodyParsers.parse.json) { _ => Results.Ok }
  }

  private def authRetrievals(affinityGroup: AffinityGroup) =
    Future.successful(new ~(Some("id"), Some(affinityGroup)))

  private val agentAffinityGroup = AffinityGroup.Agent
  private val orgAffinityGroup = AffinityGroup.Organisation
  private val noEnrollment = Enrolments(Set())

  "Auth Action" when {

    "Agent user" must {

      "allow user to continue" in {

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(agentAffinityGroup)), cc.parsers.default)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        app.stop()
      }

    }

    "Org user with no enrolments" must {

      "allow user to continue" in {

        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(orgAffinityGroup)), cc.parsers.default)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        app.stop()
      }

    }

    "Individual user" must {

      "redirect the user to the unauthorised page" in {
        
        val authAction = new AuthenticatedIdentifierAction(new FakeAuthConnector(authRetrievals(Individual)), cc.parsers.default)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)
        status(result) mustBe UNAUTHORIZED

        app.stop()
      }

    }

    "the user hasn't logged in" must {

      "redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken), cc.parsers.default)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        app.stop()
      }
    }

    "the user's session has expired" must {

      "redirect the user to log in " in {

        val authAction = new AuthenticatedIdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired), cc.parsers.default)
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        app.stop()
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}


class FakeAuthConnector(stubbedRetrievalResult: Future[_]) extends AuthConnector {

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
    stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
  }

}


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

package controllers.actions

import com.google.inject.Inject
import play.api.libs.json.JsValue
import play.api.mvc.{Action, BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import util.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends BaseSpec {

  private val cc = stubControllerComponents()

  class Harness(authAction: IdentifierAction) {
    def onSubmit(): Action[JsValue] = authAction.apply(cc.parsers.json) { _ => Results.Ok }
  }

  private def authRetrievals(affinityGroup: AffinityGroup) =
    Future.successful(new ~(Some("id"), Some(affinityGroup)))

  private val agentAffinityGroup = AffinityGroup.Agent
  private val orgAffinityGroup = AffinityGroup.Organisation

  private def actionToTest(authConnector: AuthConnector) = {

    import scala.concurrent.ExecutionContext.Implicits._

    new AuthenticatedIdentifierAction(authConnector, application.injector.instanceOf[BodyParsers.Default])
  }

  "Auth Action" when {
    "Agent user" must {
      "allow user to continue" in {
        val authAction = actionToTest(new FakeAuthConnector(authRetrievals(agentAffinityGroup)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        application.stop()
      }
    }

    "Org user with no enrolments" must {
      "allow user to continue" in {
        val authAction = actionToTest(new FakeAuthConnector(authRetrievals(orgAffinityGroup)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe OK

        application.stop()
      }
    }

    "Individual user" must {
      "redirect the user to the unauthorised page" in {
        val authAction = actionToTest(new FakeAuthConnector(authRetrievals(Individual)))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)
        status(result) mustBe UNAUTHORIZED

        application.stop()
      }
    }

    "the user hasn't logged in" must {
      "redirect the user to log in " in {
        val authAction = actionToTest(new FakeFailingAuthConnector(new MissingBearerToken))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        application.stop()
      }
    }

    "the user's session has expired" must {
      "redirect the user to log in " in {
        val authAction = actionToTest(new FakeFailingAuthConnector(new BearerTokenExpired))
        val controller = new Harness(authAction)
        val result = controller.onSubmit()(fakeRequest)

        status(result) mustBe UNAUTHORIZED

        application.stop()
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
    stubbedRetrievalResult.map(_.asInstanceOf[A])
  }
}

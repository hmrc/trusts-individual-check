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

package util

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

import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, Inside}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits._

class BaseSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar
  with BeforeAndAfter
  with GuiceOneServerPerSuite
  with Inside {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private val bodyParsers = stubControllerComponents().parsers.defaultBodyParser

  lazy val application = applicationBuilder().build()

  def applicationBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(bodyParsers, Organisation))
      )
      .configure(
        Seq(
          "metrics.enabled" -> false,
          "auditing.enabled" -> false): _*
      )
  }

  def fakeRequest : FakeRequest[JsValue] = FakeRequest("POST", "")
    .withHeaders(CONTENT_TYPE -> "application/json")
    .withBody(Json.parse("{}"))

  def postRequestWithPayload(payload: JsValue, withDraftId: Boolean = true): FakeRequest[JsValue] = {
    if (withDraftId) {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(payload)
    } else {
      FakeRequest("POST", "/trusts/register")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(payload)
    }
  }
}







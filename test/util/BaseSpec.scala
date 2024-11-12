/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.actions.{FakeIdentifierAction, IdentifierAction}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.{Injector, bind}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.IndividualCheckRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.WireMockSupport
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, containing, post, urlEqualTo}

import scala.concurrent.ExecutionContext

class BaseSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar
  with GuiceOneServerPerSuite
  with WireMockSupport {

  lazy val injector: Injector = app.injector
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = injector.instanceOf[ExecutionContext]

  private val bodyParsers = stubControllerComponents().parsers.defaultBodyParser

  lazy val application: Application = applicationBuilder().build()

  val mockIndividualCheckRepository: IndividualCheckRepository = mock[IndividualCheckRepository]
  val mockAuditService: AuditService = mock[AuditService]

  def applicationBuilder(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(bodyParsers, Organisation)),
        bind[IndividualCheckRepository].toInstance(mockIndividualCheckRepository),
        bind[AuditService].toInstance(mockAuditService)
      )
      .configure(
        "metrics.enabled" -> false,
        "auditing.enabled" -> false,
        "microservice.services.auth.port" -> wireMockServer.port(),
        "microservice.services.individual-match.port" -> wireMockServer.port()
      )
  }

  def fakeRequest: FakeRequest[JsValue] = FakeRequest("POST", "")
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

  def createMockForIndividualMatchUrl(returnStatus: Int, errorResponse: JsValue, individualsMatchUrl: String = "/individuals/match"): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(individualsMatchUrl)).willReturn(
        aResponse()
          .withStatus(returnStatus)
          .withBody(errorResponse.toString())
      )
    )

  def createMockForIndividualMatchUrlNoBody(returnStatus: Int, individualsMatchUrl: String = "/individuals/match"): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(individualsMatchUrl)).willReturn(
        aResponse()
          .withStatus(returnStatus)
      )
    )

  def createMockForIndividualMatchUrlWithHeaders(returnStatus: Int, responseBody: String, individualsMatchUrl: String = "/individuals/match"): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(individualsMatchUrl))
        .withHeader(CONTENT_TYPE, containing("application/json"))
        .withHeader("Environment", containing("dev"))
        .willReturn(
          aResponse()
            .withStatus(returnStatus)
            .withBody(responseBody)
        )
    )

}







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

package suite

import models.IdMatchRequest
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar

trait BaseSuite extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  val idString = "IDSTRING"

  val matchSuccessBody = """{"individualMatch":true}"""

  val internalServerErrorBody: String =
    """{
      |  "failures": [
      |    {
      |      "code": "SERVER_ERROR",
      |      "reason": "IF is currently experiencing problems that require live service intervention."
      |    }
      |  ]
      |}""".stripMargin

  val matchErrorBody: String =
    """{
      |  "failures": [
      |    {
      |      "code":"RESOURCE_NOT_FOUND",
      |      "reason":"The remote endpoint has indicated that no data can be found."
      |    }
      |  ]
      |}""".stripMargin

  val successRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456A", "Name", "Name", "2000-01-01")

  val notFoundRequest: IdMatchRequest =
    IdMatchRequest(idString, "AB123456C", "Name", "Name", "2000-01-01")

}

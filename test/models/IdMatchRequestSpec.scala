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

package models

import org.scalatest.matchers.must.{Matchers => MustMatchers}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class IdMatchRequestSpec extends AnyWordSpec with MustMatchers{

  private val exampleJson:String = """{"id":"ID","nino":"AB123456A","surname":"Bloggs","forename":"Joe","birthDate":"2000-02-29"}"""

  private val exampleObj:IdMatchRequest = IdMatchRequest(
    id = "ID",
    nino = "AB123456A",
    surname = "Bloggs",
    forename = "Joe",
    birthDate = "2000-02-29")

  "exampleRequest" should {

    "read correctly" in {
      val json = Json.parse(exampleJson)
      val obj = Json.fromJson[IdMatchRequest](json).get
      obj.mustBe(exampleObj)
    }

    "write correctly" in {
      val json:JsValue = Json.toJson(exampleObj)
      Json.stringify(json).mustBe(exampleJson)
    }
  }
}

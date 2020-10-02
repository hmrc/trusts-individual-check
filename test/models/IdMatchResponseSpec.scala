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

package models

import org.scalatest.matchers.must.{Matchers => MustMatchers}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}

class IdMatchResponseSpec extends AnyWordSpec  with MustMatchers{

  private val exampleSuccessJson:String = """{"id":"ID","idMatch":true}"""

  private val exampleSuccess:IdMatchResponse = IdMatchResponse(id = "ID", idMatch = true)

  private val exampleErrorJson:String = """{"errors":["Something went wrong"]}"""
  private val exampleErrorObj:IdMatchError = IdMatchError(errors = Seq("Something went wrong"))

  "successful Response" should {

    "read correctly" in {
      val json = Json.parse(exampleSuccessJson)
      val obj = Json.fromJson[IdMatchResponse](json).get
      obj.mustBe(exampleSuccess)
    }

    "write correctly" in {
      val json:JsValue = Json.toJson(exampleSuccess)
      Json.stringify(json).mustBe(exampleSuccessJson)
    }
  }

  "error Response" should {

    "read correctly" in {
      val json = Json.parse(exampleErrorJson)
      val obj = Json.fromJson[IdMatchError](json).get
      obj.mustBe(exampleErrorObj)
    }

    "write correctly" in {
      val json:JsValue = Json.toJson(exampleErrorObj)
      Json.stringify(json).mustBe(exampleErrorJson)
    }
  }
}

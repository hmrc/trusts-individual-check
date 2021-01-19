/*
 * Copyright 2021 HM Revenue & Customs
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

package models.api1585

import play.api.libs.json.JsValue
import org.scalatest.matchers.must.{Matchers => MustMatchers}
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class IdMatchApiResponseSpec extends AnyWordSpec  with MustMatchers{

  private val exampleSuccessJson:String = """{"individualMatch":true}"""

  private val exampleSuccess:IdMatchApiResponseSuccess = IdMatchApiResponseSuccess(individualMatch = true)

  "successful Response" should {

    "read correctly" in {
      val json = Json.parse(exampleSuccessJson)
      val obj = Json.fromJson[IdMatchApiResponseSuccess](json).get
      obj.mustBe(exampleSuccess)
    }

    "write correctly" in {
      val json:JsValue = Json.toJson(exampleSuccess)
      Json.stringify(json).mustBe(exampleSuccessJson)
    }
  }
}

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

import models.IdMatchRequest
import play.api.libs.json.{JsValue, Json}

trait IdentityMatchHelper {

  val idString = "IDSTRING"

  val matchSuccess: JsValue = Json.parse("""{"individualMatch":true}""")
  val matchFailure: JsValue = Json.parse("""{"individualMatch":false}""")

  val genericIdMatchRequest: IdMatchRequest = IdMatchRequest(idString, "AB123456A", "Bob", "Dog", "2000-01-01")

}


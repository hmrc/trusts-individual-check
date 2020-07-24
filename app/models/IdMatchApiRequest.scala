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

import play.api.libs.json.{JsObject, OWrites, Reads, __}

final case class IdMatchApiRequest(nino: String, surname: String, forename: String, birthDate: String)

object IdMatchApiRequest {

  implicit lazy val reads: Reads[IdMatchApiRequest] = {
    import play.api.libs.functional.syntax._
    (
      ( __ \ "nino").read[String] and
      (__ \ "surname").read[String] and
      (__ \ "forename").read[String] and
      (__ \ "birthDate").read[String]
    )(IdMatchApiRequest.apply _)
  }

  implicit lazy val writes: OWrites[IdMatchApiRequest] = {
    import play.api.libs.functional.syntax._
    (
      ( __ \ "nino").write[String] and
        (__ \ "surname").write[String] and
        (__ \ "forename").write[String] and
        (__ \ "birthDate").write[String]
      )(unlift(IdMatchApiRequest.unapply))
  }
}
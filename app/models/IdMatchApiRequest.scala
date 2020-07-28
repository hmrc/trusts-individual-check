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

  private val ninoPattern = "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D\\s]?$".r

  private val surnamePattern = "^(?=.{1,99}$)([A-Z]([-'. ]{0,1}[A-Za-z ]+)*[A-Za-z]?)$".r

  private val forenamePattern = "^(?=.{1,99}$)([A-Z]([-'. ]{0,1}[A-Za-z ]+)*[A-Za-z]?)$".r

  private val birthdatePattern = """^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]
      ||11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}
      |[-]02[-](0[1-9]|1[0-9]|2[0-8])))$""".r

  implicit lazy val reads: Reads[IdMatchApiRequest] = {

    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    (
      ( __ \ "nino").read[String](Reads.pattern(ninoPattern, "Not a valid National Insurance number")) and
      (__ \ "surname").read[String](Reads.pattern(surnamePattern, "Not a valid surname")) and
      (__ \ "forename").read[String](Reads.pattern(forenamePattern, "Not a valid forename")) and
      (__ \ "birthDate").read[String](Reads.pattern(birthdatePattern, "Not a valid birthdate"))
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
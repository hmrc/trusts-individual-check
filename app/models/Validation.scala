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

package models

import play.api.libs.json.Reads

object Validation {

  private val ninoPattern = "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D\\s]?$".r

  private val surnamePattern = "^(?=.{1,99}$)([A-Z]([-'. ]{0,1}[A-Za-z ]+)*[A-Za-z]?)$".r

  private val forenamePattern = "^(?=.{1,99}$)([A-Z]([-'. ]{0,1}[A-Za-z ]+)*[A-Za-z]?)$".r

  private val birthdatePattern = """^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$""".r

  val NinoReads: Reads[String] = Reads.pattern(ninoPattern, "Not a valid National Insurance number")

  val SurnameReads: Reads[String] = Reads.pattern(surnamePattern, "Not a valid surname")

  val ForenameReads: Reads[String] = Reads.pattern(forenamePattern, "Not a valid forename")

  val DateReads: Reads[String] = Reads.pattern(birthdatePattern, "Not a valid birthdate")

}

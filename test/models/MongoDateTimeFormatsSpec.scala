/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers as MustMatchers
import play.api.libs.json.Json

import java.time.LocalDateTime

class MongoDateTimeFormatsSpec extends AnyFreeSpec with MustMatchers {

  import MongoDateTimeFormats.{localDateTimeRead, localDateTimeWrite}

  "MongoDateTimeFormats" - {

    "localDateTimeRead" - {

      "must read MongoDB date into LocalDateTime in UTC" in {

        val json = Json.obj(
          "$date" -> 1704067200000L
        )

        val expected = LocalDateTime.of(2024, 1, 1, 0, 0, 0)

        json.as[LocalDateTime] mustBe expected
      }

      "must read a date before the Unix epoch" in {

        val json = Json.obj(
          "$date" -> -1000L
        )

        val expected = LocalDateTime.of(1969, 12, 31, 23, 59, 59)

        json.as[LocalDateTime] mustBe expected
      }

      "must return JsError when date field is missing" in {

        val json = Json.obj()

        Json.fromJson[LocalDateTime](json).isError mustBe true
      }

      "must return JsError when date field is invalid" in {

        val json = Json.obj(
          "$date" -> "invalid-date"
        )

        Json.fromJson[LocalDateTime](json).isError mustBe true
      }
    }

    "localDateTimeWrite" - {

      "must write LocalDateTime as MongoDB date in UTC" in {

        val dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)

        val expected = Json.obj(
          "$date" -> 1704067200000L
        )

        Json.toJson(dateTime) mustBe expected
      }

      "must write a date before the Unix epoch" in {

        val dateTime = LocalDateTime.of(1969, 12, 31, 23, 59, 59)

        val expected = Json.obj(
          "$date" -> -1000L
        )

        Json.toJson(dateTime) mustBe expected
      }
    }

    "read and write" - {

      "must preserve LocalDateTime during JSON round trip" in {

        val original = LocalDateTime.of(
          2024, 6, 15, 12, 30, 45, 123000000
        )

        val json = Json.toJson(original)

        val result = json.as[LocalDateTime]

        result mustBe original
      }
    }
  }

}

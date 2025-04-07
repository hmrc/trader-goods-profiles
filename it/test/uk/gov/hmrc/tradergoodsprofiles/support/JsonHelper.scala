/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofiles.support

import play.api.libs.json.Json

trait JsonHelper {

  val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"

  def createExpectedJson(code: String, message: String, errorNumber: Option[String] = None): Any =
    if (errorNumber.isDefined) {
      Json.obj(
        "correlationId" -> correlationId,
        "code" -> code,
        "message" -> message,
        "errorNumber" -> errorNumber
      )
    } else {
      Json.obj(
        "correlationId" -> correlationId,
        "code" -> code,
        "message" -> message
      )
    }

  def createExpectedJson(code: String, message: String): Any = Json.obj(
    "correlationId" -> correlationId,
    "code" -> code,
    "message" -> message
  )

  def createExpectedError(code: String, message: String, errorNumber: Int): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> code,
          "message"     -> message,
          "errorNumber" -> errorNumber
        )
      )
    )
}

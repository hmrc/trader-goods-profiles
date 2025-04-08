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

package uk.gov.hmrc.tradergoodsprofiles.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.ErrorResponse

class ErrorResponseSpec extends PlaySpec with GetRecordResponseSupport {

  private val correlationId = "correlationId"
  private val code = "1"
  private val message = "message"

  "toJson" should {
    "convert Object to json when no errorNumber" in {
      val errorResponseNoErrorNumberJson =
        Json.obj(
          "correlationId" -> correlationId,
          "code"          -> code,
          "message"       -> message
        )

      Json.toJson(ErrorResponse(correlationId, code, message, None, None)) mustBe errorResponseNoErrorNumberJson
    }

    "convert Object to json when errorNumber" in {
      val errorResponseErrorNumberJson =
        Json.obj(
          "correlationId" -> correlationId,
          "code"          -> code,
          "message"       -> message,
          "errorNumber" -> "errorNumber"
        )

      Json.toJson(ErrorResponse(correlationId, code, message, Some("errorNumber"), None)) mustBe errorResponseErrorNumberJson
    }
  }
}

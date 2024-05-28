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

package uk.gov.hmrc.tradergoodsprofiles.utils

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsonValidationError}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.Error
import uk.gov.hmrc.tradergoodsprofiles.utils.ValidationSupport.convertError

class ValidationSupportSpec extends PlaySpec {

  "convertError" should {
    "for actorId" in {
      val error = JsonValidationError("error.path.missing")
      val path  = JsPath \ "actorId"

      val errors         = scala.collection.Seq(
        (path, scala.collection.Seq(error))
      )
      val expectedErrors = Seq(
        Error("INVALID_REQUEST_PARAMETER", "Mandatory field actorId was missing from body or is in the wrong format", 8)
      )

      val result = convertError(errors)

      result mustBe expectedErrors
    }

    "for traderRef and comcode" in {
      val error = JsonValidationError("error.path.missing")
      val path1 = JsPath \ "traderRef"
      val path2 = JsPath \ "comcode"

      val errors         = scala.collection.Seq(
        (path1, scala.collection.Seq(error)),
        (path2, scala.collection.Seq(error))
      )
      val expectedErrors = Seq(
        Error(
          "INVALID_REQUEST_PARAMETER",
          "Mandatory field traderRef was missing from body or is in the wrong format",
          9
        ),
        Error(
          "INVALID_REQUEST_PARAMETER",
          "Mandatory field comcode was missing from body or is in the wrong format",
          11
        )
      )

      val result = convertError(errors)

      result mustBe expectedErrors
    }
  }

}

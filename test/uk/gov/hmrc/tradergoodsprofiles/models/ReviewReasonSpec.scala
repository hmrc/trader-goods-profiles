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
import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.ReviewReason._

class ReviewReasonSpec extends PlaySpec with GetRecordResponseSupport {
  "ReviewReason" must {
    "throw a JsError" when {
      "deserializing from json and not a correct ReviewReason" in {
        Json.fromJson[ReviewReason](JsString("invalid")) mustBe JsError("[ReviewReason] Reads unknown ReviewReason: invalid")
      }

      "deserializing from json and not a JsString" in {
        Json.fromJson[ReviewReason](JsBoolean(true)) mustBe JsError("[ReviewReason] Reads expected JsString but got: true")
      }
    }

    "deserialize from json" when {

      "Commodity" in {
        Json.fromJson[ReviewReason](JsString("Commodity")) mustBe JsSuccess(Commodity)
        Json.fromJson[ReviewReason](JsString("commodity")) mustBe JsSuccess(Commodity)
        Json.fromJson[ReviewReason](JsString("COMMODITY")) mustBe JsSuccess(Commodity)
      }

      "Inadequate" in {
        Json.fromJson[ReviewReason](JsString("Inadequate")) mustBe JsSuccess(Inadequate)
        Json.fromJson[ReviewReason](JsString("inadequate")) mustBe JsSuccess(Inadequate)
        Json.fromJson[ReviewReason](JsString("INADEQUATE")) mustBe JsSuccess(Inadequate)
      }

      "Unclear" in {
        Json.fromJson[ReviewReason](JsString("Unclear")) mustBe JsSuccess(Unclear)
        Json.fromJson[ReviewReason](JsString("unclear")) mustBe JsSuccess(Unclear)
        Json.fromJson[ReviewReason](JsString("UNCLEAR")) mustBe JsSuccess(Unclear)
      }

      "Measure" in {
        Json.fromJson[ReviewReason](JsString("Measure")) mustBe JsSuccess(Measure)
        Json.fromJson[ReviewReason](JsString("measure")) mustBe JsSuccess(Measure)
        Json.fromJson[ReviewReason](JsString("MEASURE")) mustBe JsSuccess(Measure)
      }

      "Mismatch" in {
        Json.fromJson[ReviewReason](JsString("Mismatch")) mustBe JsSuccess(Mismatch)
        Json.fromJson[ReviewReason](JsString("mismatch")) mustBe JsSuccess(Mismatch)
        Json.fromJson[ReviewReason](JsString("MISMATCH")) mustBe JsSuccess(Mismatch)
      }
    }

    "serialize to json" when {

      "Commodity" in {
        Json.toJson(Commodity: ReviewReason) mustBe JsString("The commodity code has expired. You'll need to change the commodity code and categorise the goods.")
      }

      "Inadequate" in {
        Json.toJson(Inadequate: ReviewReason) mustBe JsString("HMRC have reviewed this record. The goods description does not have enough detail. If you want to use this record on an IMMI, you'll need to amend the goods description")
      }

      "Unclear" in {
        Json.toJson(Unclear: ReviewReason) mustBe JsString("HMRC have reviewed the record. The goods description is unclear. If you want to use this record on an IMMI, you'll need to amend the goods description.")
      }

      "Measure" in {
        Json.toJson(Measure: ReviewReason) mustBe JsString("The commodity code or restrictions have changed. You'll need to categorise the record.")
      }

      "Mismatch" in {
        Json.toJson(Mismatch: ReviewReason) mustBe JsString("HMRC have reviewed this record. The commodity code and goods description do not match. If you want to use this record on an IMMI, you'll need to amend the commodity code and the goods description.")
      }
    }
  }
}

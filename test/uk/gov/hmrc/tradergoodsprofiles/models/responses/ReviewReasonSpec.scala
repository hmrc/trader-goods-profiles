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

package uk.gov.hmrc.tradergoodsprofiles.models.responses

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.*
import uk.gov.hmrc.tradergoodsprofiles.models.responses.ReviewReason.*

class ReviewReasonSpec extends PlaySpec {

  "ReviewReason.fromString" should {
    "return the correct ReviewReason for a valid value (case-insensitive)" in {
      ReviewReason.fromString("mismatch") mustBe Some(Mismatch)
      ReviewReason.fromString("Mismatch") mustBe Some(Mismatch)
      ReviewReason.fromString("COUNTRY") mustBe Some(Country)
    }

    "return None for an invalid value" in {
      ReviewReason.fromString("not-a-reason") mustBe None
    }
  }

  "ReviewReason JSON Writes" should {
    "convert a ReviewReason to JsString of its value" in {
      Json.toJson[ReviewReason](Measure) mustBe JsString("measure")
      Json.toJson[ReviewReason](Country) mustBe JsString("country")
    }
  }

  "ReviewReason JSON Reads" should {
    "parse a valid JsString into the correct ReviewReason" in {
      JsString("commodity").as[ReviewReason] mustBe Commodity
      JsString("INaDeQuAtE").as[ReviewReason] mustBe Inadequate
    }

    "fail to parse an unknown JsString value" in {
      val result = JsString("unknown").validate[ReviewReason]
      result.isError mustBe true
      val errors = result.asInstanceOf[JsError].errors
      errors.head._2.head.message must include ("Unknown ReviewReason: unknown")
    }

    "fail to parse a non-string JSON value" in {
      val result = JsNumber(123).validate[ReviewReason]
      result.isError mustBe true
      val errors = result.asInstanceOf[JsError].errors
      errors.head._2.head.message must include ("Expected JsString")
    }
  }

  "ReviewReason.values" should {
    "contain all expected ReviewReasons" in {
      values must contain theSameElementsAs Seq(
        Commodity,
        Inadequate,
        Unclear,
        Measure,
        Mismatch,
        Country
      )
    }
  }

  "ReviewReason JSON round-trip" should {
    "serialize and deserialize all values correctly" in {
      values.foreach { reason =>
        val json = Json.toJson[ReviewReason](reason)
        json.as[ReviewReason] mustBe reason
      }
    }
  }
}

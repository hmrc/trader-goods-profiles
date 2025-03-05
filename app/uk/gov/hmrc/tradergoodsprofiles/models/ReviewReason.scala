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

import play.api.libs.json._

sealed trait ReviewReason {
  val description: String
}

object ReviewReason {

  val values: Seq[ReviewReason] = Seq(Commodity, Inadequate, Unclear, Measure, Mismatch)

  case object Commodity extends ReviewReason {
    val description: String = "The commodity code has expired. You'll need to change the commodity code and categorise the goods."
  }

  case object Inadequate extends ReviewReason {
    val description: String = "HMRC have reviewed this record. The goods description does not have enough detail. If you want to use this record on an IMMI, you'll need to amend the goods description"
  }

  case object Unclear extends ReviewReason {
    val description: String = "HMRC have reviewed the record. The goods description is unclear. If you want to use this record on an IMMI, you'll need to amend the goods description."
  }

  case object Measure extends ReviewReason {
    val description: String = "The commodity code or restrictions have changed. You'll need to categorise the record."
  }

  case object Mismatch extends ReviewReason {
    val description: String = "HMRC have reviewed this record. The commodity code and goods description do not match. If you want to use this record on an IMMI, you'll need to amend the commodity code and the goods description."
  }

  implicit val writes: Writes[ReviewReason] = Writes(reviewReason => JsString(reviewReason.description))

  implicit val reads: Reads[ReviewReason] = Reads[ReviewReason] {
    case JsString(value) =>
      value.toLowerCase match {
        case "commodity"  => JsSuccess(Commodity)
        case "inadequate" => JsSuccess(Inadequate)
        case "unclear"    => JsSuccess(Unclear)
        case "measure"    => JsSuccess(Measure)
        case "mismatch"   => JsSuccess(Mismatch)
        case _            => JsError(s"[ReviewReason] Reads unknown ReviewReason: $value")
      }
    case other => JsError(s"[ReviewReason] Reads expected JsString but got: $other")
  }
}

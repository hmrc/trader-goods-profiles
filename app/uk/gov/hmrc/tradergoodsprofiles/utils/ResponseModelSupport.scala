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

import play.api.libs.json.{JsNull, JsObject}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.ReviewReason

object ResponseModelSupport {

  def removeNulls(jsObject: JsObject): JsObject =
    JsObject(jsObject.fields.collect {
      case (s, j: JsObject)            =>
        (s, removeNulls(j))
      case other if other._2 != JsNull =>
        other
    })

  def convertReviewReason(reviewReason: Option[String], toReview: Boolean): Option[String] = {
    val reviewReasonToMap: Option[ReviewReason] = reviewReason.flatMap(ReviewReason.fromString)
    if (!toReview) None
    else reviewReasonToMap.map(_.description)
  }
}

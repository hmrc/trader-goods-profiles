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

package uk.gov.hmrc.tradergoodsprofiles.models.requests

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.verifying
import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment

import java.time.Instant

case class APICreateRecordRequest(
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[BigDecimal] = None,
  measurementUnit: Option[String] = None,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant] = None
)

object APICreateRecordRequest {

  def nonEmptyString: Reads[String] = verifying[String](_.nonEmpty)

  implicit val reads: Reads[APICreateRecordRequest] = (
    (JsPath \ "actorId").read[String](nonEmptyString) and
      (JsPath \ "traderRef").read[String](nonEmptyString) and
      (JsPath \ "comcode").read[String](nonEmptyString) and
      (JsPath \ "goodsDescription").read[String](nonEmptyString) and
      (JsPath \ "countryOfOrigin").read[String](nonEmptyString) and
      (JsPath \ "category").read[Int](verifying[Int](category => category >= 1 && category <= 3)) and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[BigDecimal] and
      (JsPath \ "measurementUnit").readNullable[String] and
      (JsPath \ "comcodeEffectiveFromDate").read[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant]
  )(APICreateRecordRequest.apply _)

  implicit val writes: OWrites[APICreateRecordRequest] =
    Json.writes[APICreateRecordRequest]

}

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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Reads.verifying
import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment

import java.time.Instant

case class UpdateRecordRequest(
  actorId: String,
  traderRef: Option[String],
  comcode: Option[String],
  goodsDescription: Option[String],
  countryOfOrigin: Option[String],
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Option[Instant],
  comcodeEffectiveToDate: Option[Instant]
)

object UpdateRecordRequest {
  def nonEmptyString: Reads[String] = verifying[String](_.nonEmpty)

  implicit val reads: Reads[UpdateRecordRequest] = (
    (JsPath \ "actorId").read[String](nonEmptyString) and
      (JsPath \ "traderRef").readNullable[String](nonEmptyString) and
      (JsPath \ "comcode").readNullable[String](nonEmptyString) and
      (JsPath \ "goodsDescription").readNullable[String](nonEmptyString) and
      (JsPath \ "countryOfOrigin").readNullable[String](nonEmptyString) and
      (JsPath \ "category").readNullable[Int](verifying[Int](category => category >= 1 && category <= 3)) and
      (JsPath \ "assessments").readNullable[Seq[Assessment]] and
      (JsPath \ "supplementaryUnit").readNullable[BigDecimal] and
      (JsPath \ "measurementUnit").readNullable[String] and
      (JsPath \ "comcodeEffectiveFromDate").readNullable[Instant] and
      (JsPath \ "comcodeEffectiveToDate").readNullable[Instant]
  )(UpdateRecordRequest.apply _)

  implicit val writes: OWrites[UpdateRecordRequest] = Json.writes[UpdateRecordRequest]

  implicit val format: OFormat[UpdateRecordRequest] = OFormat(reads, writes)
}

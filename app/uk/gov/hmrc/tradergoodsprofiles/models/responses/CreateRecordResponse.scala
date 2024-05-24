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

package uk.gov.hmrc.tradergoodsprofiles.models.response

import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment

import java.time.Instant

case class CreateRecordResponse(
  eori: String,
  actorId: String,
  recordId: String,
  traderRef: String,
  comcode: String,
  accreditationStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[Int],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: String,
  nirmsNumber: String,
  niphlNumber: String,
  locked: Boolean,
  srcSystemName: String,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object CreateRecordResponse {

  implicit val createRecordResponseReads: Reads[CreateRecordResponse] = (json: JsValue) =>
    JsSuccess(
      CreateRecordResponse(
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "recordId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "accreditationStatus").as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").as[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[Int],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").as[String],
        (json \ "nirmsNumber").as[String],
        (json \ "niphlNumber").as[String],
        (json \ "locked").as[Boolean],
        (json \ "srcSystemName").as[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val createRecordResponseWrites: Writes[CreateRecordResponse] =
    (createRecordResponse: CreateRecordResponse) =>
      Json.obj(
        "eori"                     -> createRecordResponse.eori,
        "actorId"                  -> createRecordResponse.actorId,
        "recordId"                 -> createRecordResponse.recordId,
        "traderRef"                -> createRecordResponse.traderRef,
        "comcode"                  -> createRecordResponse.comcode,
        "accreditationStatus"      -> createRecordResponse.accreditationStatus,
        "goodsDescription"         -> createRecordResponse.goodsDescription,
        "countryOfOrigin"          -> createRecordResponse.countryOfOrigin,
        "category"                 -> createRecordResponse.category,
        "assessments"              -> createRecordResponse.assessments,
        "supplementaryUnit"        -> createRecordResponse.supplementaryUnit,
        "measurementUnit"          -> createRecordResponse.measurementUnit,
        "comcodeEffectiveFromDate" -> createRecordResponse.comcodeEffectiveFromDate,
        "comcodeEffectiveToDate"   -> createRecordResponse.comcodeEffectiveToDate,
        "version"                  -> createRecordResponse.version,
        "active"                   -> createRecordResponse.active,
        "toReview"                 -> createRecordResponse.toReview,
        "reviewReason"             -> createRecordResponse.reviewReason,
        "declarable"               -> createRecordResponse.declarable,
        "ukimsNumber"              -> createRecordResponse.ukimsNumber,
        "nirmsNumber"              -> createRecordResponse.nirmsNumber,
        "niphlNumber"              -> createRecordResponse.niphlNumber,
        "locked"                   -> createRecordResponse.locked,
        "srcSystemName"            -> createRecordResponse.srcSystemName,
        "createdDateTime"          -> createRecordResponse.createdDateTime,
        "updatedDateTime"          -> createRecordResponse.updatedDateTime
      )
}

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

import play.api.libs.json.*
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment
import uk.gov.hmrc.tradergoodsprofiles.utils.ResponseModelSupport.removeNulls

import java.time.Instant

case class CreateOrUpdateRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  adviceStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: Option[String],
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object CreateOrUpdateRecordResponse {

  implicit val createRecordResponseReads: Reads[CreateOrUpdateRecordResponse] = (json: JsValue) =>
    JsSuccess(
      CreateOrUpdateRecordResponse(
        (json \ "recordId").as[String],
        (json \ "eori").as[String],
        (json \ "actorId").as[String],
        (json \ "traderRef").as[String],
        (json \ "comcode").as[String],
        (json \ "adviceStatus").as[String],
        (json \ "goodsDescription").as[String],
        (json \ "countryOfOrigin").as[String],
        (json \ "category").asOpt[Int],
        (json \ "assessments").asOpt[Seq[Assessment]],
        (json \ "supplementaryUnit").asOpt[BigDecimal],
        (json \ "measurementUnit").asOpt[String],
        (json \ "comcodeEffectiveFromDate").as[Instant],
        (json \ "comcodeEffectiveToDate").asOpt[Instant],
        (json \ "version").as[Int],
        (json \ "active").as[Boolean],
        (json \ "toReview").as[Boolean],
        (json \ "reviewReason").asOpt[String],
        (json \ "declarable").as[String],
        (json \ "ukimsNumber").asOpt[String],
        (json \ "nirmsNumber").asOpt[String],
        (json \ "niphlNumber").asOpt[String],
        (json \ "createdDateTime").as[Instant],
        (json \ "updatedDateTime").as[Instant]
      )
    )

  implicit val createRecordResponseWrites: Writes[CreateOrUpdateRecordResponse] =
    (createRecordResponse: CreateOrUpdateRecordResponse) =>
      removeNulls(
        Json.obj(
          "eori"                     -> createRecordResponse.eori,
          "actorId"                  -> createRecordResponse.actorId,
          "recordId"                 -> createRecordResponse.recordId,
          "traderRef"                -> createRecordResponse.traderRef,
          "comcode"                  -> createRecordResponse.comcode,
          "adviceStatus"             -> createRecordResponse.adviceStatus,
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
          "createdDateTime"          -> createRecordResponse.createdDateTime,
          "updatedDateTime"          -> createRecordResponse.updatedDateTime
        )
      )

}

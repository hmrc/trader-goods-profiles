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

import java.time.Instant

case class CreateRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  accreditationStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Seq[Assessment],
  supplementaryUnit: Int,
  measurementUnit: String,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Instant,
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: String,
  nirmsNumber: String,
  niphlNumber: String,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object CreateRecordResponse {
  implicit lazy val format: OFormat[CreateRecordResponse] = new OFormat[CreateRecordResponse] {
    override def writes(o: CreateRecordResponse): JsObject =
      Json.obj(
        "recordId"                 -> o.recordId,
        "eori"                     -> o.eori,
        "actorId"                  -> o.actorId,
        "traderRef"                -> o.traderRef,
        "comcode"                  -> o.comcode,
        "accreditationStatus"      -> o.accreditationStatus,
        "goodsDescription"         -> o.goodsDescription,
        "countryOfOrigin"          -> o.countryOfOrigin,
        "category"                 -> o.category,
        "assessments"              -> o.assessments,
        "supplementaryUnit"        -> o.supplementaryUnit,
        "measurementUnit"          -> o.measurementUnit,
        "comcodeEffectiveFromDate" -> o.comcodeEffectiveFromDate,
        "comcodeEffectiveToDate"   -> o.comcodeEffectiveToDate,
        "version"                  -> o.version,
        "active"                   -> o.active,
        "toReview"                 -> o.toReview,
        "reviewReason"             -> o.reviewReason,
        "declarable"               -> o.declarable,
        "ukimsNumber"              -> o.ukimsNumber,
        "nirmsNumber"              -> o.nirmsNumber,
        "niphlNumber"              -> o.niphlNumber,
        "createdDateTime"          -> o.createdDateTime,
        "updatedDateTime"          -> o.updatedDateTime
      )

    override def reads(json: JsValue): JsResult[CreateRecordResponse] = for {
      recordId                 <- (json \ "recordId").validate[String]
      eori                     <- (json \ "eori").validate[String]
      actorId                  <- (json \ "actorId").validate[String]
      traderRef                <- (json \ "traderRef").validate[String]
      comcode                  <- (json \ "comcode").validate[String]
      accreditationStatus      <- (json \ "accreditationStatus").validate[String]
      goodsDescription         <- (json \ "goodsDescription").validate[String]
      countryOfOrigin          <- (json \ "countryOfOrigin").validate[String]
      category                 <- (json \ "category").validate[Int]
      assessments              <- (json \ "assessments").validate[Seq[Assessment]]
      supplementaryUnit        <- (json \ "supplementaryUnit").validate[Int]
      measurementUnit          <- (json \ "measurementUnit").validate[String]
      comcodeEffectiveFromDate <- (json \ "comcodeEffectiveFromDate").validate[Instant]
      comcodeEffectiveToDate   <- (json \ "comcodeEffectiveToDate").validate[Instant]
      version                  <- (json \ "version").validate[Int]
      active                   <- (json \ "active").validate[Boolean]
      toReview                 <- (json \ "toReview").validate[Boolean]
      reviewReason             <- (json \ "reviewReason").validateOpt[String]
      declarable               <- (json \ "declarable").validate[String]
      ukimsNumber              <- (json \ "ukimsNumber").validate[String]
      nirmsNumber              <- (json \ "nirmsNumber").validate[String]
      niphlNumber              <- (json \ "niphlNumber").validate[String]
      createdDateTime          <- (json \ "createdDateTime").validate[Instant]
      updatedDateTime          <- (json \ "updatedDateTime").validate[Instant]
    } yield CreateRecordResponse(
      recordId,
      eori,
      actorId,
      traderRef,
      comcode,
      accreditationStatus,
      goodsDescription,
      countryOfOrigin,
      category,
      assessments,
      supplementaryUnit,
      measurementUnit,
      comcodeEffectiveFromDate,
      comcodeEffectiveToDate,
      version,
      active,
      toReview,
      reviewReason,
      declarable,
      ukimsNumber,
      nirmsNumber,
      niphlNumber,
      createdDateTime,
      updatedDateTime
    )
  }
}

package uk.gov.hmrc.tradergoodsprofiles.models

import play.api.libs.json.{Json, OFormat}
import java.time.Instant

case class CreateRecordResponse(
  recordId: String,
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: Int,
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
  implicit val format: OFormat[CreateRecordResponse] = Json.format[CreateRecordResponse]
}

package uk.gov.hmrc.tradergoodsprofiles.models

import play.api.libs.json.{Json, OFormat}

import java.time.Instant

case class CreateRecordRequest(
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Seq[Assessment],
  supplementaryUnit: Int,
  measurementUnit: String,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Instant
)

object CreateRecordRequest {
  implicit val format: OFormat[CreateRecordRequest] = Json.format[CreateRecordRequest]
}

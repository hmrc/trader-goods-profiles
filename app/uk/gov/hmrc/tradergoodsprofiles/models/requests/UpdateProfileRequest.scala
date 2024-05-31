package uk.gov.hmrc.tradergoodsprofiles.models.requests

import play.api.libs.json.{Json, OFormat}

case class UpdateProfileRequest(
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object UpdateProfileRequest {
  implicit val format: OFormat[UpdateProfileRequest] = Json.format[UpdateProfileRequest]
}

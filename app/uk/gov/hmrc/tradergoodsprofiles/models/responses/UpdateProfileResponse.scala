package uk.gov.hmrc.tradergoodsprofiles.models.responses

import play.api.libs.json.{Json, OFormat}

case class UpdateProfileResponse(
  eori: String,
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: String,
  niphlNumber: String
)

object UpdateProfileResponse {
  implicit val format: OFormat[UpdateProfileResponse] = Json.format[UpdateProfileResponse]
}

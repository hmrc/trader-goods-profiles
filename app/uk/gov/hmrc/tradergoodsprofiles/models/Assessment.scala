package uk.gov.hmrc.tradergoodsprofiles.models

import play.api.libs.json.{Json, OFormat}

case class Assessment(
  assessmentId: String,
  primaryCategory: Int,
  condition: Condition
)

object Assessment {
  implicit val format: OFormat[Assessment] = Json.format[Assessment]
}

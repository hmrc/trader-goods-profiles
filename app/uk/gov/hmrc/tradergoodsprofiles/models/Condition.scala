package uk.gov.hmrc.tradergoodsprofiles.models

import play.api.libs.json.{Json, OFormat}

case class Condition(
  `type`: String,
  conditionId: String,
  conditionDescription: String,
  conditionTraderText: String
)

object Condition {
  implicit val format: OFormat[Condition] = Json.format[Condition]
}

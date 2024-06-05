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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.verifying
import play.api.libs.json._

case class MaintainProfileRequest(
  actorId: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String]
)

object MaintainProfileRequest {

  def nonEmptyString: Reads[String] = verifying[String](_.nonEmpty)

  implicit val reads: Reads[MaintainProfileRequest] = (
    (JsPath \ "actorId").read[String](nonEmptyString) and
      (JsPath \ "ukimsNumber").read[String](nonEmptyString) and
      (JsPath \ "nirmsNumber").readNullable[String] and
      (JsPath \ "niphlNumber").readNullable[String]
  )(MaintainProfileRequest.apply _)

  implicit val writes: OWrites[MaintainProfileRequest] = Json.writes[MaintainProfileRequest]

  implicit val format: OFormat[MaintainProfileRequest] = OFormat(reads, writes)
}

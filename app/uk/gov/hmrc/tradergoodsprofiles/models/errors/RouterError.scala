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

package uk.gov.hmrc.tradergoodsprofiles.models.errors

import play.api.libs.json.{JsPath, Json, Reads, Writes}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService.DateTimeFormat

import java.time.Instant

case class RouterError(
  correlationId: String,
  code: String,
  message: String,
  timestamp: Option[Instant] = None
)

object RouterError {
  implicit val read: Reads[RouterError] = Json.reads[RouterError]

  implicit val write: Writes[RouterError] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "timestamp").writeOptionWithNull[String]
  )(e =>
    (
      e.correlationId,
      e.code,
      e.message,
      e.timestamp.fold(Some(Instant.now.asStringSeconds))(o => Some(o.asStringSeconds))
    )
  )
}

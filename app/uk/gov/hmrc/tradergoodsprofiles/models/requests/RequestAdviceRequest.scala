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

import scala.util.matching.Regex

case class RequestAdviceRequest(
  actorId: String,
  requestorName: String,
  requestorEmail: String
)

object RequestAdviceRequest {
  private val actorIdPattern: Regex            = raw"[A-Z]{2}\d{12,15}".r
  def isValidActorId(actorId: String): Boolean = actorIdPattern.matches(actorId)
  def nonEmptyString: Reads[String]            = verifying[String](_.nonEmpty)

  val validActorId: Reads[String] = verifying(isValidActorId)

  implicit val reads: Reads[RequestAdviceRequest] = (
    (JsPath \ "actorId").read[String](validActorId) and
      (JsPath \ "requestorName").read[String](nonEmptyString) and
      (JsPath \ "requestorEmail").read[String](nonEmptyString)
  )(RequestAdviceRequest.apply _)

  implicit val writes: OWrites[RequestAdviceRequest] = Json.writes[RequestAdviceRequest]

  implicit val format: OFormat[RequestAdviceRequest] = OFormat(reads, writes)
}

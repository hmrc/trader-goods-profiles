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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.{BadRequest, Forbidden, InternalServerError, Unauthorized}
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService.DateTimeFormat

import java.time.Instant

sealed trait ErrorResponse {
  val timestamp: Instant
  val code: String
  val message: String

  def toResult: Result
}

case class ForbiddenErrorResponse(timestamp: Instant, message: String) extends ErrorResponse {
  override val code: String = "FORBIDDEN"

  def toResult: Result = Forbidden(Json.toJson(ForbiddenErrorResponse(timestamp, message)))
}

object ForbiddenErrorResponse {
  implicit val read: Reads[ForbiddenErrorResponse] = Json.reads[ForbiddenErrorResponse]

  implicit val write: Writes[ForbiddenErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

case class UnauthorisedErrorResponse(timestamp: Instant, message: String) extends ErrorResponse {
  override val code: String = "UNAUTHORIZED"

  def toResult: Result = Unauthorized(Json.toJson(UnauthorisedErrorResponse(timestamp, message)))
}

object UnauthorisedErrorResponse {
  implicit val read: Reads[UnauthorisedErrorResponse] = Json.reads[UnauthorisedErrorResponse]

  implicit val write: Writes[UnauthorisedErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

case class ServerErrorResponse(timestamp: Instant, message: String) extends ErrorResponse {
  override val code: String = "INTERNAL_SERVER_ERROR"

  def toResult: Result =
    InternalServerError(Json.toJson(ServerErrorResponse(timestamp, message)))
}

object ServerErrorResponse {
  implicit val read: Reads[ServerErrorResponse] = Json.reads[ServerErrorResponse]

  implicit val write: Writes[ServerErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

case class InvalidRecordIdErrorResponse(timestamp: Instant, message: String) extends ErrorResponse {
  override val code: String = "INVALID_RECORD_ID_PARAMETER"

  def toResult: Result = BadRequest(Json.toJson(InvalidRecordIdErrorResponse(timestamp, message)))
}

case class InvalidActorIdErrorResponse(timestamp: Instant, message: String) extends ErrorResponse {
  override val code: String = "INVALID_ACTOR_ID_PARAMETER"

  def toResult: Result = BadRequest(Json.toJson(InvalidActorIdErrorResponse(timestamp, message)))
}

object InvalidRecordIdErrorResponse {
  implicit val read: Reads[InvalidRecordIdErrorResponse] = Json.reads[InvalidRecordIdErrorResponse]

  implicit val write: Writes[InvalidRecordIdErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

object InvalidActorIdErrorResponse {
  implicit val read: Reads[InvalidActorIdErrorResponse] = Json.reads[InvalidActorIdErrorResponse]

  implicit val write: Writes[InvalidActorIdErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
    )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

case class InvalidHeaderErrorResponse(
  timestamp: Instant,
  message: String
) extends ErrorResponse {
  override val code: String = "INVALID_HEADER_PARAMETERS"

  def toResult: Result = Forbidden(Json.toJson(InvalidHeaderErrorResponse(timestamp, message)))
}

object InvalidHeaderErrorResponse {
  implicit val read: Reads[InvalidHeaderErrorResponse] = Json.reads[InvalidHeaderErrorResponse]

  implicit val write: Writes[InvalidHeaderErrorResponse] = (
    (JsPath \ "timestamp").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp.asStringSeconds, e.code, e.message))
}

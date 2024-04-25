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

package uk.gov.hmrc.tradergoodsprofiles.models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.{Forbidden, InternalServerError, Unauthorized}

import java.time.Instant

trait AuthErrorResponse {
  val timestamp: Instant
  val code: String
  val message: String

  def toResult: Result
}

case class ForbiddenError(timestamp: Instant, message: String) extends AuthErrorResponse {
  override val code: String = "FORBIDDEN"

  def toResult: Result = Forbidden(Json.toJson(ForbiddenError(timestamp, message)))
}

object ForbiddenError {
  implicit val read: Reads[ForbiddenError] = Json.reads[ForbiddenError]

  implicit val write: Writes[ForbiddenError] = (
    (JsPath \ "timestamp").write[Instant] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp, e.code, e.message))
}

case class UnauthorisedError(timestamp: Instant, message: String) extends AuthErrorResponse {
  override val code: String = "UNAUTHORIZED"

  def toResult: Result = Unauthorized(Json.toJson(UnauthorisedError(timestamp, message)))
}

object UnauthorisedError {
  implicit val read: Reads[UnauthorisedError] = Json.reads[UnauthorisedError]

  implicit val write: Writes[UnauthorisedError] = (
    (JsPath \ "timestamp").write[Instant] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp, e.code, e.message))
}

case class ServerError(timestamp: Instant, message: String) extends AuthErrorResponse {
  override val code: String = "INTERNAL_SERVER_ERROR"

  def toResult: Result = InternalServerError(Json.toJson(ServerError(timestamp, message)))
}

object ServerError {
  implicit val read: Reads[ServerError] = Json.reads[ServerError]

  implicit val write: Writes[ServerError] = (
    (JsPath \ "timestamp").write[Instant] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.timestamp, e.code, e.message))
}

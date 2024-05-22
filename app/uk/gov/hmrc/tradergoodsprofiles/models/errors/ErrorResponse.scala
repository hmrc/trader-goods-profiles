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

sealed trait ErrorResponse {
  val correlationId: String
  val code: String
  val message: String
  val errors: Option[Seq[Error]] = None

  def toResult: Result
}

case class ForbiddenErrorResponse(correlationId: String, message: String) extends ErrorResponse {
  override val code: String = "FORBIDDEN"

  def toResult: Result = Forbidden(Json.toJson(ForbiddenErrorResponse(correlationId, message)))
}

object ForbiddenErrorResponse {
  implicit val read: Reads[ForbiddenErrorResponse] = Json.reads[ForbiddenErrorResponse]

  implicit val write: Writes[ForbiddenErrorResponse] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.correlationId, e.code, e.message))
}

case class UnauthorisedErrorResponse(correlationId: String, message: String) extends ErrorResponse {
  override val code: String = "UNAUTHORIZED"

  def toResult: Result = Unauthorized(Json.toJson(UnauthorisedErrorResponse(correlationId, message)))
}

object UnauthorisedErrorResponse {
  implicit val read: Reads[UnauthorisedErrorResponse] = Json.reads[UnauthorisedErrorResponse]

  implicit val write: Writes[UnauthorisedErrorResponse] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.correlationId, e.code, e.message))
}

case class ServerErrorResponse(correlationId: String, message: String) extends ErrorResponse {
  override val code: String = "INTERNAL_SERVER_ERROR"

  def toResult: Result =
    InternalServerError(Json.toJson(ServerErrorResponse(correlationId, message)))
}

object ServerErrorResponse {
  implicit val read: Reads[ServerErrorResponse] = Json.reads[ServerErrorResponse]

  implicit val write: Writes[ServerErrorResponse] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String]
  )(e => (e.correlationId, e.code, e.message))
}

case class InvalidErrorResponse(correlationId: String, code: String, message: String, errorNumber: Int)
    extends ErrorResponse {
  override val errors  = Some(Seq(Error(code, message, errorNumber)))
  def toResult: Result = BadRequest(Json.toJson(InvalidErrorResponse(correlationId, code, message, errorNumber)))
}

object InvalidErrorResponse {
  implicit val read: Reads[InvalidErrorResponse] = Json.reads[InvalidErrorResponse]

  implicit val write: Writes[InvalidErrorResponse] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "errors").writeOptionWithNull[Seq[Error]]
  )(e => (e.correlationId, "BAD_REQUEST", "Bad Request", e.errors))
}

case class InvalidHeaderErrorResponse(
  correlationId: String,
  code: String,
  message: String,
  errorNumber: Int
) extends ErrorResponse {
  override val errors  = Some(Seq(Error(code, message, errorNumber)))
  def toResult: Result = BadRequest(Json.toJson(InvalidHeaderErrorResponse(correlationId, code, message, errorNumber)))
}

object InvalidHeaderErrorResponse {
  implicit val read: Reads[InvalidHeaderErrorResponse] = Json.reads[InvalidHeaderErrorResponse]

  implicit val write: Writes[InvalidHeaderErrorResponse] = (
    (JsPath \ "correlationId").write[String] and
      (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "errors").writeOptionWithNull[Seq[Error]]
  )(e => (e.correlationId, "BAD_REQUEST", "Bad Request", e.errors))
}

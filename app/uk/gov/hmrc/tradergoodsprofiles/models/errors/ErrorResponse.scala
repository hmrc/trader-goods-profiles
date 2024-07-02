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

import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.{Forbidden, InternalServerError, Unauthorized}

case class ErrorResponse(
  correlationId: String,
  code: String,
  message: String,
  errors: Option[Seq[Error]] = None
)

object ErrorResponse {
  implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]

  def serverErrorResponse(correlationId: String, message: String): ErrorResponse =
    ErrorResponse(
      correlationId,
      "INTERNAL_SERVER_ERROR",
      message
    )

}

case class ServiceError(status: Int, errorResponse: ErrorResponse)

case class ForbiddenErrorResponse(correlationId: String, message: String) {
  def toResult: Result =
    Forbidden(
      Json.toJson(
        ErrorResponse(
          correlationId,
          "FORBIDDEN",
          message
        )
      )
    )
}

case class UnauthorisedErrorResponse(correlationId: String, message: String) {
  def toResult: Result =
    Unauthorized(
      Json.toJson(
        ErrorResponse(
          correlationId,
          "UNAUTHORIZED",
          message
        )
      )
    )
}

case class ServerErrorResponse(correlationId: String, message: String) {
  def toResult: Result =
    InternalServerError(
      Json.toJson(
        ErrorResponse(
          correlationId,
          "INTERNAL_SERVER_ERROR",
          message
        )
      )
    )
}

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

package uk.gov.hmrc.tradergoodsprofiles.connectors

import play.api.http.Status.{INTERNAL_SERVER_ERROR, isSuccessful}
import play.api.libs.Files.logger
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

trait RouterHttpReader {

  def uuidService: UuidService

  implicit def httpReader[T](implicit reads: Reads[T], tt: TypeTag[T]): HttpReads[Either[ServiceError, T]] =
    new HttpReads[Either[ServiceError, T]] {
      override def read(method: String, url: String, response: HttpResponse): Either[ServiceError, T] =
        response match {
          case response if isSuccessful(response.status) =>
            jsonAs[T](response.body).left.map(ServiceError(INTERNAL_SERVER_ERROR, _))
          case response                                  =>
            Left(handleErrors(response))
        }
    }

  implicit def httpReaderWithoutResponseBody: HttpReads[Either[ServiceError, Int]] =
    new HttpReads[Either[ServiceError, Int]] {
      override def read(method: String, url: String, response: HttpResponse): Either[ServiceError, Int] =
        response match {
          case response if isSuccessful(response.status) => Right(response.status)
          case response                                  => Left(handleErrors(response))
        }
    }

  private def jsonAs[T](responseBody: String)(implicit reads: Reads[T], tt: TypeTag[T]): Either[ErrorResponse, T] =
    Try(Json.parse(responseBody)) match {
      case Success(value)     =>
        value.validate[T] match {
          case JsSuccess(v, _) => Right(v)
          case JsError(error)  =>
            logger.warn(
              s"[RouterHttpReader] - Response body could not be read as type ${typeOf[T]}, error ${error.toString()}"
            )
            Left(
              ErrorResponse
                .serverErrorResponse(
                  uuidService.uuid,
                  s"Response body could not be read as type ${typeOf[T]}"
                )
            )
        }
      case Failure(exception) =>
        logger.warn(
          s"[RouterHttpReader] - Response body could not be parsed as JSON, body: $responseBody",
          exception
        )
        Left(
          ErrorResponse
            .serverErrorResponse(
              uuidService.uuid,
              s"Response body could not be parsed as JSON, body: $responseBody"
            )
        )
    }

  private def handleErrors(response: HttpResponse): ServiceError =
    jsonAs[ErrorResponse](response.body)
      .fold(
        ServiceError(INTERNAL_SERVER_ERROR, _),
        ServiceError(response.status, _)
      )

}

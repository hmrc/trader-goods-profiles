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

package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{BaseController, Request, Result}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

trait ValidationRules {
  this: BaseController =>

  def uuidService: UuidService

  protected def validateContentType(implicit request: Request[_]): Either[Error, _] =
    request.headers
      .get("Content-Type")
      .filter(_ == "application/json")
      .toRight(
        Error(InvalidHeaderParameter, InvalidHeaderContentTypeMessage, InvalidHeaderContentType)
      )

  protected def validateAcceptHeader(implicit request: Request[_]): Either[Error, String] = {
    val pattern = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

    request.headers
      .get(HeaderNames.ACCEPT)
      .filter(pattern.matches(_))
      .toRight(Error(InvalidHeaderParameter, InvalidHeaderAcceptMessage, InvalidHeaderAccept))

  }

  protected def validateClientIdHeader(implicit request: Request[_]): Either[Error, String] =
    request.headers
      .get(XClientIdHeader)
      .toRight(Error(InvalidHeaderParameter, InvalidHeaderClientIdMessage, InvalidHeaderClientId))

  protected def validateAllHeader(implicit request: Request[_]): Either[Result, _] =
    (for {
      _ <- validateAcceptHeader
      _ <- validateContentType
      _ <- validateClientIdHeader
    } yield ()).left
      .map(e => createBadRequestResponse(e.code, e.message, e.errorNumber))
  protected def validateAcceptAndClientIdHeader(implicit
    request: Request[_]
  ): Either[Result, _]                                                             =
    (for {
      _ <- validateAcceptHeader
      _ <- validateClientIdHeader
    } yield ()).left
      .map(e => createBadRequestResponse(e.code, e.message, e.errorNumber))

  private def createBadRequestResponse(code: String, message: String, errorNumber: Int): Result =
    BadRequest(
      Json.toJson(
        ErrorResponse(
          uuidService.uuid,
          "BAD_REQUEST",
          "Bad Request",
          Some(Seq(Error(code, message, errorNumber)))
        )
      )
    )
}

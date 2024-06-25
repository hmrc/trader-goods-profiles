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

import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.http.HttpVerbs
import uk.gov.hmrc.tradergoodsprofiles.models.errors.BadRequestErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

class ValidateHeaderAction @Inject() (uuidService: UuidService)(implicit ec: ExecutionContext)
    extends ActionFilter[Request] {

  override val executionContext: ExecutionContext = ec
  private lazy val pattern                        = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

  override def filter[A](request: Request[A]): Future[Option[Result]] =
    for {
      isValidAcceptHeader <- validateAcceptHeader(request)
      isValidContentType  <- validateContentTypeHeader(request)
      isValidClientId     <- validateClientIdHeader(request)
    } yield (isValidAcceptHeader, isValidContentType, isValidClientId) match {
      case (true, true, true) => None
      case (false, _, _)      => invalidAcceptHeaderResult
      case (_, false, _)      => invalidContentTypeResult
      case _                  => invalidClientIdResult
    }

  private def invalidAcceptHeaderResult                                  =
    Some(
      BadRequestErrorResponse(
        uuidService.uuid,
        InvalidHeaderParameter,
        InvalidHeaderAcceptMessage,
        InvalidHeaderAccept
      ).toResult
    )
  private def validateAcceptHeader(request: Request[_]): Future[Boolean] =
    request.headers.get(HeaderNames.ACCEPT) match {
      case Some(header) if pattern.matches(header) => successful(true)
      case _                                       => successful(false)
    }

  private def invalidContentTypeResult =
    Some(
      BadRequestErrorResponse(
        uuidService.uuid,
        InvalidHeaderParameter,
        InvalidHeaderContentTypeMessage,
        InvalidHeaderContentType
      ).toResult
    )

  private def validateContentTypeHeader(request: Request[_]): Future[Boolean] =
    request.method match {
      case HttpVerbs.DELETE =>
        successful(true)
      case _                =>
        request.headers.get(HeaderNames.CONTENT_TYPE) match {
          case Some(header) if header.equals(MimeTypes.JSON) => successful(true)
          case _                                             => successful(false)
        }
    }

  private def invalidClientIdResult =
    Some(
      BadRequestErrorResponse(
        uuidService.uuid,
        InvalidHeaderParameter,
        InvalidHeaderClientIdMessage,
        InvalidHeaderClientId
      ).toResult
    )

  private def validateClientIdHeader(request: Request[_]): Future[Boolean] =
    request.headers.get(XClientIdHeader) match {
      case Some(_) => successful(true)
      case _       => successful(false)
    }

}

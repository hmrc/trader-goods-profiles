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

import cats.data.EitherT
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.tradergoodsprofiles.config.Constants
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{InvalidErrorResponse, InvalidHeaderErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ValidateAction @Inject()(uuidService: UuidService)(implicit ec: ExecutionContext)
    extends ActionFilter[Request] {

  override val executionContext: ExecutionContext = ec
  private lazy val pattern                        = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

  override def filter[A](request: Request[A]): Future[Option[Result]] = {

    val result = for {
      _ <- validateAcceptHeader(request)
      _ <- validateContentTypeHeader(request)
      _ <- validateClientIdHeader(request)
    } yield None

    result.merge
  }

  private def validateAcceptHeader(request: Request[_]): EitherT[Future, Option[Result], Unit] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.ACCEPT) match {
        case Some(header) if pattern.matches(header) => Some(())
        case _                                       => None
      },
      Some(
        InvalidHeaderErrorResponse(
          uuidService.uuid,
          InvalidHeaderParameter,
          InvalidHeaderAcceptMessage,
          InvalidHeaderAccept
        ).toResult
      )
    )

  private def validateContentTypeHeader(request: Request[_]): EitherT[Future, Option[Result], Unit] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.CONTENT_TYPE) match {
        case Some(header) if header.equals(MimeTypes.JSON) => Some(())
        case _                                             => None
      },
      Some(
        InvalidHeaderErrorResponse(
          uuidService.uuid,
          InvalidHeaderParameter,
          InvalidHeaderContentTypeMessage,
          InvalidHeaderContentType
        ).toResult
      )
    )

  private def validateClientIdHeader(request: Request[_]): EitherT[Future, Option[Result], Unit] =
    EitherT.fromOption(
      request.headers.get(Constants.XClientIdHeader) match {
        case Some(_) => Some(())
        case _       => None
      },
      Some(
        InvalidHeaderErrorResponse(
          uuidService.uuid,
          InvalidHeaderParameter,
          InvalidHeaderClientIdMessage,
          InvalidHeaderClientId
        ).toResult
      )
    )

   def validateRecordId(recordId: String): EitherT[Future, Result, String] =
    EitherT.fromEither[Future](
      Try(UUID.fromString(recordId).toString).toEither.left.map(_ =>
        InvalidErrorResponse(
          uuidService.uuid,
          InvalidRequestParameter,
          InvalidRecordIdMessage,
          InvalidRecordId
        ).toResult
      )
    )
}

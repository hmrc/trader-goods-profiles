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
import uk.gov.hmrc.tradergoodsprofiles.models.errors.InvalidHeaderErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateHeaderAction @Inject() (datetimeService: DateTimeService)(implicit ec: ExecutionContext)
    extends ActionFilter[Request] {

  override val executionContext: ExecutionContext = ec

  override def filter[A](request: Request[A]): Future[Option[Result]] = {

    val result = for {
      _ <- validateAcceptHeader(request)
      _ <- validateContentTypeHeader(request)
    } yield None

    result.merge
  }

  private def validateAcceptHeader(request: Request[_]): EitherT[Future, Option[Result], Unit] = {
    val pattern = """^application/vnd[.]{1}hmrc[.]{1}1{1}[.]0[+]{1}json$""".r

    EitherT.fromOption(
      request.headers.get(HeaderNames.ACCEPT) match {
        case Some(header) if pattern.matches(header) => Some(())
        case _                                       => None
      },
      Some(InvalidHeaderErrorResponse(datetimeService.timestamp, "Accept header is missing or invalid").toResult)
    )
  }

  private def validateContentTypeHeader(request: Request[_]): EitherT[Future, Option[Result], Unit] =
    EitherT.fromOption(
      request.headers.get(HeaderNames.CONTENT_TYPE) match {
        case Some(header) if header.equals(MimeTypes.JSON) => Some(())
        case _                                             => None
      },
      Some(
        InvalidHeaderErrorResponse(datetimeService.timestamp, "Content-Type header is missing or invalid").toResult
      )
    )

}

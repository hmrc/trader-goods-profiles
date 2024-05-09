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

package uk.gov.hmrc.tradergoodsprofiles.services

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.models.GetRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.models.errors.ServerErrorResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {

  def getRecord(eori: String, recordId: String)(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordResponse]
}

class RouterServiceImpl @Inject() (
  routerConnector: RouterConnector,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends RouterService
    with Logging {

  def getRecord(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetRecordResponse] =
    EitherT(
      routerConnector
        .get(eori, recordId)
        .map {
          case value if is2xx(value.status) => jsonAs[GetRecordResponse](value)
          case value                        => Left(Status(value.status)(value.body))
        }
        .recover { case UpstreamErrorResponse(message, status, _, _) =>
          Left(Status(status)(message))
        }
    )

  private def jsonAs[T](response: HttpResponse)(implicit reads: Reads[T], tt: TypeTag[T]) =
    Try(Json.parse(response.body)) match {
      case Success(value)     =>
        value.validate[T] match {
          case JsSuccess(v, _) => Right(v)
          case JsError(_)      =>
            logger.error(s"[RouterServiceImpl] - Response body could not be read as type ${typeOf[T]}")
            Left(
              ServerErrorResponse(
                dateTimeService.timestamp,
                s"Response body could not be read as type ${typeOf[T]}"
              ).toResult
            )
        }
      case Failure(exception) =>
        logger.error(
          s"[RouterServiceImpl] - Response body could not be parsed as JSON, body: ${response.body}",
          exception
        )
        Left(
          ServerErrorResponse(
            dateTimeService.timestamp,
            s"Response body could not be parsed as JSON, body: ${response.body}"
          ).toResult
        )
    }
}

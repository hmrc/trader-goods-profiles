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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{RouterError, ServerErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.{CreateRecordRequest, CreateRecordResponse, GetRecordResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {

  def getRecord(eori: String, recordId: String)(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordResponse]
  def createRecord(eori: String, createRequest: CreateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateRecordResponse]
}

class RouterServiceImpl @Inject() (
  routerConnector: RouterConnector,
  dateTimeService: DateTimeService
)(implicit ec: ExecutionContext)
    extends RouterService
    with Logging {

  def getRecord(eoriNumber: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetRecordResponse] =
    EitherT(
      routerConnector
        .get(eoriNumber, recordId)
        .map {
          case httpResponse if is2xx(httpResponse.status) => jsonAs[GetRecordResponse](httpResponse.body)
          case httpResponse                               =>
            Left(handleError(httpResponse.body, httpResponse.status, Some(eoriNumber), Some(recordId)))
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"[RouterServiceImpl] - Exception when retrieving record for eori number $eoriNumber and record ID $recordId, with message ${ex.getMessage}",
            ex
          )
          Left(
            ServerErrorResponse(
              dateTimeService.timestamp,
              s"Could not retrieve record for eori number $eoriNumber and record ID $recordId"
            ).toResult
          )
        }
    )

  def createRecord(eori: String, createRequest: CreateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateRecordResponse] =
    EitherT(
      routerConnector
        .post(eori, createRequest)
        .map { httpResponse =>
          httpResponse.status match {
            case status if is2xx(status) =>
              jsonAs[CreateRecordResponse](httpResponse.body)
            case _                       =>
              Left(handleError(httpResponse.body, httpResponse.status))
          }
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"[RouterServiceImpl] - Exception when creating record for eori number $eori with message ${ex.getMessage}",
            ex
          )
          Left(
            ServerErrorResponse(dateTimeService.timestamp, "Could not create record due to an internal error").toResult
          )
        }
    )

  private def handleError(
    responseBody: String,
    status: Int,
    eoriNumber: Option[String] = None,
    recordId: Option[String] = None
  ): Result = {
    val errorContext = (eoriNumber, recordId) match {
      case (Some(eori), Some(record)) => s"for eori number '$eori' and record ID '$record'"
      case (Some(eori), None)         => s"for eori number '$eori'"
      case _                          => ""
    }
    logger.error(
      s"[RouterServiceImpl] - Error processing request $errorContext, status '$status' with message: $responseBody"
    )
    jsonAs[RouterError](responseBody).fold(
      error => error,
      routerError => Status(status)(Json.toJson(routerError.copy(timestamp = Some(dateTimeService.timestamp))))
    )
  }

  private def jsonAs[T](responseBody: String)(implicit reads: Reads[T], tt: TypeTag[T]) =
    Try(Json.parse(responseBody)) match {
      case Success(value)     =>
        value.validate[T] match {
          case JsSuccess(v, _) => Right(v)
          case JsError(error)  =>
            logger.error(
              s"[RouterServiceImpl] - Response body could not be read as type ${typeOf[T]}, error ${error.toString()}"
            )
            Left(
              ServerErrorResponse(
                dateTimeService.timestamp,
                s"Response body could not be read as type ${typeOf[T]}"
              ).toResult
            )
        }
      case Failure(exception) =>
        logger.error(
          s"[RouterServiceImpl] - Response body could not be parsed as JSON, body: $responseBody",
          exception
        )
        Left(
          ServerErrorResponse(
            dateTimeService.timestamp,
            s"Response body could not be parsed as JSON, body: $responseBody"
          ).toResult
        )
    }
}

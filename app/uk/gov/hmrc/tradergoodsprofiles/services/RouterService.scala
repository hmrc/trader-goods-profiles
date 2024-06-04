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
import uk.gov.hmrc.tradergoodsprofiles.models.requests.router.RouterUpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.models.requests.{APICreateRecordRequest, APIUpdateProfileRequest, UpdateRecordRequest, router}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{CreateOrUpdateRecordResponse, GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.UpdateProfileResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[RouterServiceImpl])
trait RouterService {

  def getRecord(eori: String, recordId: String)(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordResponse]

  def createRecord(eori: String, createRequest: APICreateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateOrUpdateRecordResponse]

  def updateRecord(eori: String, recordId: String, updateRequest: UpdateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateOrUpdateRecordResponse]

  def removeRecord(eori: String, recordId: String, actorId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit]

  def getRecords(eori: String, lastUpdatedDate: Option[String], page: Option[Int], size: Option[Int])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetRecordsResponse]

  def updateProfile(eori: String, updateRequest: APIUpdateProfileRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, UpdateProfileResponse]]
}

class RouterServiceImpl @Inject() (
  routerConnector: RouterConnector,
  uuidService: UuidService
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
              uuidService.uuid,
              s"Could not retrieve record for eori number $eoriNumber and record ID $recordId"
            ).toResult
          )
        }
    )

  override def removeRecord(eoriNumber: String, recordId: String, actorId: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, Unit] =
    EitherT(
      routerConnector
        .put(eoriNumber, recordId, actorId)
        .map {
          case httpResponse if is2xx(httpResponse.status) => Right(())
          case httpResponse                               =>
            Left(handleError(httpResponse.body, httpResponse.status, Some(eoriNumber), Some(recordId)))
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"[RouterServiceImpl] - Exception when removing record for eori number $eoriNumber and record ID $recordId, with message ${ex.getMessage}",
            ex
          )
          Left(
            ServerErrorResponse(
              uuidService.uuid,
              s"Could not remove record for eori number $eoriNumber and record ID $recordId"
            ).toResult
          )
        }
    )

  def getRecords(eoriNumber: String, lastUpdatedDate: Option[String], page: Option[Int], size: Option[Int])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, GetRecordsResponse] =
    EitherT(
      routerConnector
        .getRecords(eoriNumber, lastUpdatedDate, page, size)
        .map {
          case httpResponse if is2xx(httpResponse.status) => jsonAs[GetRecordsResponse](httpResponse.body)
          case httpResponse                               => Left(handleError(httpResponse.body, httpResponse.status, Some(eoriNumber)))
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"[RouterServiceImpl] - Exception when retrieving record for eori number $eoriNumber, with message ${ex.getMessage}",
            ex
          )
          Left(
            ServerErrorResponse(
              uuidService.uuid,
              s"Could not retrieve record for eori number $eoriNumber"
            ).toResult
          )
        }
    )

  def createRecord(eori: String, createRequest: APICreateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateOrUpdateRecordResponse] = {
    val routerCreateRecordRequest = router.RouterCreateRecordRequest(eori, createRequest)
    EitherT(
      routerConnector
        .post(routerCreateRecordRequest)
        .map { httpResponse =>
          httpResponse.status match {
            case status if is2xx(status) =>
              jsonAs[CreateOrUpdateRecordResponse](httpResponse.body)
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
            ServerErrorResponse(uuidService.uuid, "Could not create record due to an internal error").toResult
          )
        }
    )
  }

  def updateRecord(eori: String, recordId: String, updateRequest: UpdateRecordRequest)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, Result, CreateOrUpdateRecordResponse] = {
    val routerUpdateRecordRequest = RouterUpdateRecordRequest(eori, recordId, updateRequest)
    EitherT(
      routerConnector
        .put(routerUpdateRecordRequest)
        .map { httpResponse =>
          httpResponse.status match {
            case status if is2xx(status) =>
              jsonAs[CreateOrUpdateRecordResponse](httpResponse.body)
            case _                       =>
              Left(handleError(httpResponse.body, httpResponse.status))
          }
        }
        .recover { case ex: Throwable =>
          logger.error(
            s"[RouterServiceImpl] - Exception when updating record for eori number $eori with message ${ex.getMessage}",
            ex
          )
          Left(
            ServerErrorResponse(uuidService.uuid, "Could not update record due to an internal error").toResult
          )
        }
    )
  }

  def updateProfile(eori: String, updateRequest: APIUpdateProfileRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[Result, UpdateProfileResponse]] =
    routerConnector
      .put(eori, updateRequest)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[UpdateProfileResponse](httpResponse.body)
          case _                       =>
            Left(handleError(httpResponse.body, httpResponse.status, Some(eori)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(s"Exception when updating profile for eori number $eori: ${ex.getMessage}", ex)
        Left(ServerErrorResponse(uuidService.uuid, "Could not update profile due to an internal error").toResult)
      }

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
    jsonAs[RouterError](responseBody)
      .fold(
        error => error,
        routerError => Status(status)(Json.toJson(routerError))
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
                uuidService.uuid,
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
            uuidService.uuid,
            s"Response body could not be parsed as JSON, body: $responseBody"
          ).toResult
        )
    }

}

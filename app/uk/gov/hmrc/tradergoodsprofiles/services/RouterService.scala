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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{CreateOrUpdateRecordResponse, GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.{TypeTag, typeOf}
import scala.util.{Failure, Success, Try}

class RouterService @Inject() (
  routerConnector: RouterConnector,
  uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends Logging {

  def getRecord(eoriNumber: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, GetRecordResponse]] =
    routerConnector
      .get(eoriNumber, recordId)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[GetRecordResponse](httpResponse.body).fold(
              error =>
                Left(
                  ServiceError(
                    INTERNAL_SERVER_ERROR,
                    error
                  )
                ),
              response => Right(response)
            )
          case _                       =>
            Left(handleErrors(httpResponse, Some(eoriNumber), Some(recordId)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when retrieving record for eori number $eoriNumber and record ID $recordId, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                s"Could not retrieve record for eori number $eoriNumber and record ID $recordId"
              )
          )
        )
      }

  def removeRecord(eoriNumber: String, recordId: String, request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, Int]] =
    routerConnector
      .removeRecord(eoriNumber, recordId, request)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            Right(status)
          case _                       =>
            Left(
              handleErrors(httpResponse, Some(eoriNumber), Some(recordId), Some(actorId))
            )
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when removing record for eori number $eoriNumber, record ID $recordId, and actor ID $actorId, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                s"Could not remove record for eori number $eoriNumber, record ID $recordId, and actor ID $actorId"
              )
          )
        )
      }

  def getRecords(eoriNumber: String, lastUpdatedDate: Option[String], page: Option[Int], size: Option[Int])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, GetRecordsResponse]] =
    routerConnector
      .getRecords(eoriNumber, lastUpdatedDate, page, size)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[GetRecordsResponse](httpResponse.body).fold(
              error =>
                Left(
                  ServiceError(
                    INTERNAL_SERVER_ERROR,
                    error
                  )
                ),
              response => Right(response)
            )
          case _                       =>
            Left(handleErrors(httpResponse, Some(eoriNumber)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when retrieving record for eori number $eoriNumber, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(uuidService.uuid, s"Could not retrieve record for eori number $eoriNumber")
          )
        )
      }

  def createRecord(eori: String, request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, CreateOrUpdateRecordResponse]] =
    routerConnector
      .createRecord(eori, request)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[CreateOrUpdateRecordResponse](httpResponse.body).fold(
              error =>
                Left(
                  ServiceError(
                    INTERNAL_SERVER_ERROR,
                    error
                  )
                ),
              response => Right(response)
            )
          case _                       =>
            Left(handleErrors(httpResponse, Some(eori)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when creating record for eori number $eori with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(uuidService.uuid, "Could not create record due to an internal error")
          )
        )
      }

  def updateRecord(eori: String, recordId: String, request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, CreateOrUpdateRecordResponse]] =
    routerConnector
      .updateRecord(eori, recordId, request)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[CreateOrUpdateRecordResponse](httpResponse.body).fold(
              error =>
                Left(
                  ServiceError(
                    INTERNAL_SERVER_ERROR,
                    error
                  )
                ),
              updateRecordResponse => Right(updateRecordResponse)
            )
          case _                       =>
            Left(handleErrors(httpResponse, Some(eori), Some(recordId)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when updating record for eori number $eori with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(uuidService.uuid, "Could not update record due to an internal error")
          )
        )
      }

  def requestAdvice(
    eori: String,
    recordId: String,
    request: Request[JsValue]
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, Int]] =
    routerConnector
      .requestAdvice(eori, recordId, request)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            Right(status)
          case _                       =>
            Left(
              handleErrors(httpResponse, Some(eori), Some(recordId))
            )
        }
      }
      .recover { case ex: Throwable =>
        logger.error(
          s"[RouterService] - Exception when requesting advice for eori number $eori with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(uuidService.uuid, "Could not request advice due to an internal error")
          )
        )
      }

  def updateProfile(eori: String, updateRequest: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, MaintainProfileResponse]] =
    routerConnector
      .routerMaintainProfile(eori, updateRequest)
      .map { httpResponse =>
        httpResponse.status match {
          case status if is2xx(status) =>
            jsonAs[MaintainProfileResponse](httpResponse.body).fold(
              error =>
                Left(
                  ServiceError(
                    INTERNAL_SERVER_ERROR,
                    error
                  )
                ),
              response => Right(response)
            )
          case _                       =>
            Left(handleErrors(httpResponse, Some(eori)))
        }
      }
      .recover { case ex: Throwable =>
        logger.error(s"Exception when updating profile for eori number $eori: ${ex.getMessage}", ex)
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse.serverErrorResponse(
              uuidService.uuid,
              "Could not update profile due to an internal error"
            )
          )
        )
      }

  private def handleErrors(
    response: HttpResponse,
    eoriNumber: Option[String] = None,
    recordId: Option[String] = None,
    actorId: Option[String] = None
  ): ServiceError = {
    val errorContext = (eoriNumber, recordId, actorId) match {
      case (Some(eori), Some(record), Some(actor)) =>
        s"for eori number '$eori', record ID '$record', and actor ID '$actor'"
      case (Some(eori), Some(record), None)        => s"for eori number '$eori' and record ID '$record'"
      case (Some(eori), None, None)                => s"for eori number '$eori'"
      case _                                       => ""
    }

    logger.error(
      s"[RouterService] - Error processing request $errorContext, status '${response.status}' with message: ${response.body}"
    )
    jsonAs[ErrorResponse](response.body)
      .fold(
        error =>
          ServiceError(
            INTERNAL_SERVER_ERROR,
            error
          ),
        routerError =>
          ServiceError(
            response.status,
            routerError
          )
      )
  }

  private def jsonAs[T](responseBody: String)(implicit reads: Reads[T], tt: TypeTag[T]) =
    Try(Json.parse(responseBody)) match {
      case Success(value)     =>
        value.validate[T] match {
          case JsSuccess(v, _) => Right(v)
          case JsError(error)  =>
            logger.error(
              s"[RouterService] - Response body could not be read as type ${typeOf[T]}, error ${error.toString()}"
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
        logger.error(
          s"[RouterService] - Response body could not be parsed as JSON, body: $responseBody",
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

}

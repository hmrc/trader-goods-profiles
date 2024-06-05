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

package uk.gov.hmrc.tradergoodsprofiles.controllers

import cats.data.EitherT
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class GetRecordsController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getRecord(eori: String, recordId: String): Action[AnyContent] =
    (authAction(eori) andThen validateHeaderAction).async { implicit request =>
      (for {
        _      <- validateRecordId(recordId)
        record <- sendGetRecord(eori, recordId)
      } yield Ok(Json.toJson(record))).merge
    }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] =
    (authAction(eori) andThen validateHeaderAction).async { implicit request =>
      (for {
        _      <- validateQueryParameterLastUpdatedDate(lastUpdatedDate)
        record <- sendGetRecords(eori, lastUpdatedDate, page, size)
      } yield Ok(Json.toJson(record))).merge
    }

  private def validateQueryParameterLastUpdatedDate(
    lastUpdatedDate: Option[String]
  ): EitherT[Future, Result, Option[Instant]] = {
    val eitherResult: Try[Option[Instant]] = Try(lastUpdatedDate.map(dateTime => Instant.parse(dateTime)))
    EitherT.fromEither[Future](
      eitherResult match {
        case Success(instantOpt) => Right(instantOpt)
        case Failure(_)          =>
          val errorResponse = ErrorResponse.badRequestErrorResponse(
            uuidService.uuid,
            Some(Seq(Error(InvalidRequestParameter, InvalidLastUpdatedDate, InvalidLastUpdatedDateCode)))
          )
          Left(BadRequest(Json.toJson(errorResponse)))
      }
    )
  }

  private def validateRecordId(recordId: String): EitherT[Future, Result, String] = {
    val eitherResult: Try[String] = Try(UUID.fromString(recordId).toString)
    eitherResult match {
      case Success(validRecordId) => EitherT.rightT[Future, Result](validRecordId)
      case Failure(_)             =>
        val errorResponse = ErrorResponse.badRequestErrorResponse(
          uuidService.uuid,
          Some(Seq(Error(InvalidRequestParameter, InvalidRecordIdMessage, InvalidRecordId)))
        )
        EitherT.leftT[Future, String](BadRequest(Json.toJson(errorResponse)))
    }
  }

  private def sendGetRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordResponse] =
    EitherT(routerService.getRecord(eori, recordId)).leftMap(r => Status(r.status)(Json.toJson(r.errorResponse)))

  private def sendGetRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordsResponse] =
    EitherT(routerService.getRecords(eori, lastUpdatedDate, page, size)).leftMap(r =>
      Status(r.status)(Json.toJson(r.errorResponse))
    )
}

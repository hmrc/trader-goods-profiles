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

import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
      validateRecordId(recordId) match {
        case Left(errorResponse)      =>
          Future.successful(BadRequest(Json.toJson(errorResponse)))
        case Right(validatedRecordId) =>
          routerService.getRecord(eori, validatedRecordId).map {
            case Left(serviceError) =>
              Status(serviceError.status)(toJson(serviceError.errorResponse))
            case Right(response)    =>
              Ok(Json.toJson(response))
          }
      }
    }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] =
    (authAction(eori) andThen validateHeaderAction).async { implicit request =>
      validateQueryParameterLastUpdatedDate(lastUpdatedDate) match {
        case Left(errorResponse)  =>
          Future.successful(BadRequest(Json.toJson(errorResponse)))
        case Right(validatedDate) =>
          routerService.getRecords(eori, validatedDate, page, size).map {
            case Left(serviceError) =>
              Status(serviceError.status)(toJson(serviceError.errorResponse))
            case Right(response)    =>
              Ok(Json.toJson(response))
          }
      }
    }

  private def validateQueryParameterLastUpdatedDate(
    lastUpdatedDate: Option[String]
  ): Either[ErrorResponse, Option[String]] =
    Try(lastUpdatedDate.map(dateTime => Instant.parse(dateTime).toString)).toEither.left.map { _ =>
      ErrorResponse.badRequestErrorResponse(
        uuidService.uuid,
        Some(Seq(Error(InvalidRequestParameter, InvalidLastUpdatedDate, InvalidLastUpdatedDateCode)))
      )
    }

  private def validateRecordId(recordId: String): Either[ErrorResponse, String] =
    Try(UUID.fromString(recordId).toString).toEither.left.map { _ =>
      ErrorResponse.badRequestErrorResponse(
        uuidService.uuid,
        Some(Seq(Error(InvalidRequestParameter, InvalidRecordIdMessage, InvalidRecordId)))
      )
    }
}

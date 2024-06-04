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

import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.RemoveRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._
import uk.gov.hmrc.tradergoodsprofiles.utils.ValidationSupport.validateRequestBody

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class RemoveRecordController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {
  def removeRecord(eori: String, recordId: String): Action[JsValue] =
    (authAction(eori) andThen validateHeaderAction).async(parse.json) { implicit request =>
      validateRequestBody[RemoveRecordRequest](request.body, uuidService) match {
        case Left(errorResponse)        =>
          Future.successful(BadRequest(Json.toJson(errorResponse)))
        case Right(removeRecordRequest) =>
          validateRecordId(recordId) match {
            case Right(validRecordId) =>
              routerService.removeRecord(eori, validRecordId, removeRecordRequest.actorId).map {
                case Left(serviceError) =>
                  Status(serviceError.status)(toJson(serviceError.errorResponse))
                case Right(_)           =>
                  Ok
              }
            case Left(errorResponse)  =>
              Future.successful(BadRequest(Json.toJson(errorResponse)))
          }
      }
    }

  def validateRecordId(recordId: String): Either[ErrorResponse, String] =
    Try(UUID.fromString(recordId).toString).toEither.left.map(_ =>
      ErrorResponse.badRequestErrorResponse(
        uuidService.uuid,
        Some(Seq(Error(InvalidRequestParameter, InvalidRecordIdMessage, InvalidRecordId)))
      )
    )
}

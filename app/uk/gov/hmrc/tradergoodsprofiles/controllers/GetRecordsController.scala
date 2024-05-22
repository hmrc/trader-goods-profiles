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
import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.InvalidErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.{InvalidRecordId, InvalidRecordIdMessage, InvalidRequestParameter}

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
      (for {
        _      <- validateRecordId(recordId)
        record <- routerService.getRecord(eori, recordId)
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
        record <- routerService.getRecords(eori, lastUpdatedDate, page, size)
      } yield Ok(Json.toJson(record))).merge
    }

  private def validateRecordId(recordId: String): EitherT[Future, Result, String] =
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

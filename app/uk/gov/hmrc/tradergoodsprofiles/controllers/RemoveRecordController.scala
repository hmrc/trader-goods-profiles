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
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
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
import scala.util.{Failure, Success, Try}

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
      (for {
        updateRecordRequest <- validateBody(request)
        validRecordId       <- validateRecordId(recordId)
        response            <- sendRemove(eori, validRecordId, updateRecordRequest)
      } yield Ok(Json.toJson(response))).merge
    }

  private def validateBody(request: Request[JsValue]): EitherT[Future, Result, RemoveRecordRequest] =
    EitherT
      .fromEither[Future](validateRequestBody[RemoveRecordRequest](request.body, uuidService))
      .leftMap(r => BadRequest(Json.toJson(r)))

  def validateRecordId(recordId: String): EitherT[Future, Result, String] = {
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

  private def sendRemove(
    eori: String,
    recordId: String,
    removeRecordRequest: RemoveRecordRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, Int] =
    EitherT(routerService.removeRecord(eori, recordId, removeRecordRequest.actorId)).leftMap(r =>
      Status(r.status)(Json.toJson(r.errorResponse))
    )

}

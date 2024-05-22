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
import cats.implicits.toBifunctorOps
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateAction}
import uk.gov.hmrc.tradergoodsprofiles.models.RemoveRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.models.errors.InvalidErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveRecordController @Inject() (
  authAction: AuthAction,
  validateAction: ValidateAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {
  def removeRecord(eori: String, recordId: String): Action[JsValue] =
    (authAction(eori) andThen validateAction).async(parse.json) { implicit request =>
      val result = for {
        removeRecordRequest <- removeRecordRequest(request)
        _                   <- validateAction.validateRecordId(recordId)
        _                   <- routerService.removeRecord(eori, recordId, removeRecordRequest.actorId)
      } yield Ok

      result.merge
    }

  private def removeRecordRequest(
    request: Request[JsValue]
  )(implicit ec: ExecutionContext): EitherT[Future, Result, RemoveRecordRequest] =
    EitherT.fromEither(
      request.body.validate[RemoveRecordRequest].asEither.leftMap { errors =>
        InvalidErrorResponse(uuidService.uuid, InvalidRequestParameter, InvalidActorMessage, InvalidActorId).toResult
      }
    )

}

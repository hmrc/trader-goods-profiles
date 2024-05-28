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
import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.BadRequestErrorsResponse
import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ValidationSupport.convertError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def updateRecord(eori: String, recordId: String): Action[JsValue] =
    (authAction(eori) andThen validateHeaderAction).async(parse.json) { implicit request =>
      (for {
        updateRecordRequest <- validateUpdateRecordRequest(request.body)
        response            <- routerService.updateRecord(eori, recordId, updateRecordRequest)
      } yield Ok(Json.toJson(response))).merge
    }

  private def validateUpdateRecordRequest(json: JsValue)(implicit
    ec: ExecutionContext
  ): EitherT[Future, Result, UpdateRecordRequest] =
    EitherT.fromEither(
      json
        .validate[UpdateRecordRequest]
        .asEither
        .left
        .map((e: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) =>
          BadRequestErrorsResponse(uuidService.uuid, Some(convertError(e))).toResult
        )
    )

}
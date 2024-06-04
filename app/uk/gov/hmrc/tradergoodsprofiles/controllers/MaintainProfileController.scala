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

import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.APIUpdateProfileRequest
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaintainProfileController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def updateProfile(eori: String): Action[JsValue] =
    (authAction(eori) andThen validateHeaderAction).async(parse.json) { implicit request =>
      validateUpdateProfileRequest(request.body).fold(
        error => Future.successful(error),
        updateRequest =>
          routerService.updateProfile(eori, updateRequest).map {
            case Right(response) => Ok(Json.toJson(response))
            case Left(error)     => error
          }
      )
    }

  def validateUpdateProfileRequest(json: JsValue): Either[Result, APIUpdateProfileRequest] =
    json.validate[APIUpdateProfileRequest].asEither.left.map { errors =>
      BadRequest(
        Json.obj(
          "uuid"    -> uuidService.uuid,
          "message" -> "Invalid JSON",
          "errors"  -> JsError.toJson(errors) // TODO: Implement actual validation
        )
      )
    }
}

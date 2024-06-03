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

import com.google.inject.Singleton
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json._
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.RequestAccreditationRequest
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}
import uk.gov.hmrc.tradergoodsprofiles.utils.ValidationSupport.validateRequestBody

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RequestAccreditationsController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def requestAccreditation(eori: String, recordId: String): Action[JsValue] =
    (authAction(eori) andThen validateHeaderAction).async(parse.json) { implicit request =>
      validateRequestBody[RequestAccreditationRequest](request.body, uuidService) match {
        case Left(errorResponse)         =>
          Future.successful(BadRequest(Json.toJson(errorResponse)))
        case Right(accreditationRequest) =>
          routerService.requestAccreditation(eori, recordId, accreditationRequest).map {
            case Left(serviceError) =>
              Status(serviceError.status)(toJson(serviceError.errorResponse))
            case Right(_)           =>
              Created
          }
      }
    }
}

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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.APICreateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.services.{DateTimeService, RouterService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateRecordController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  dateTimeService: DateTimeService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def createRecord(eori: String): Action[JsValue] =
    (authAction(eori) andThen validateHeaderAction).async(parse.json) { implicit request =>
      (for {
        createRequest <- validateCreateRecordRequest(request.body)
        response      <- routerService.createRecord(eori, createRequest)
      } yield Created(Json.toJson(response))).merge
    }

  private def validateCreateRecordRequest(json: JsValue): EitherT[Future, Result, APICreateRecordRequest] =
    EitherT.fromEither[Future](
      json.validate[APICreateRecordRequest].asEither.left.map { errors =>
        val errorMessages = errors.map { case (path, validationErrors) =>
          path.toJsonString -> validationErrors.map(_.message).mkString(", ")
        }.toMap
        BadRequest(Json.obj("code" -> "INVALID JSON", "message" -> errorMessages))
      }
    )
}

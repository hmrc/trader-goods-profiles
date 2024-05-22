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

import cats.implicits._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateAction}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GetRecordsController @Inject() (
  authAction: AuthAction,
  validateAction: ValidateAction,
  uuidService: UuidService,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getRecord(eori: String, recordId: String): Action[AnyContent] =
    (authAction(eori) andThen validateAction).async { implicit request =>
      (for {
        _      <- validateAction.validateRecordId(recordId)
        record <- routerService.getRecord(eori, recordId)
      } yield Ok(Json.toJson(record))).merge
    }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] =
    (authAction(eori) andThen validateAction).async { implicit request =>
      (for {
        record <- routerService.getRecords(eori, lastUpdatedDate, page, size)
      } yield Ok(Json.toJson(record))).merge
    }

}

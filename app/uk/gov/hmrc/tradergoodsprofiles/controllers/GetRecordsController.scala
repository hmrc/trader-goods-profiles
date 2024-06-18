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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidateHeaderAction}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.RouterService
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetRecordsController @Inject() (
  authAction: AuthAction,
  validateHeaderAction: ValidateHeaderAction,
  routerService: RouterService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getRecord(eori: String, recordId: String): Action[AnyContent] =
    (authAction(eori) andThen validateHeaderAction).async { implicit request =>
      (for {
        record <- sendGetRecord(eori, recordId)
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
        record <- sendGetRecords(eori, lastUpdatedDate, page, size)
      } yield Ok(Json.toJson(record))).merge
    }

  private def sendGetRecord(
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordResponse] =
    EitherT(routerService.getRecord(eori, recordId)).leftMap(r => Status(r.status)(Json.toJson(r.errorResponse)))

  private def sendGetRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  )(implicit hc: HeaderCarrier): EitherT[Future, Result, GetRecordsResponse] =
    EitherT(routerService.getRecords(eori, lastUpdatedDate, page, size)).leftMap(r =>
      Status(r.status)(Json.toJson(r.errorResponse))
    )
}

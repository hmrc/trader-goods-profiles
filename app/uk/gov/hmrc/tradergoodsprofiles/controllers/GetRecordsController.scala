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
import play.api.Logging
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.GetRecordsRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetRecordsController @Inject() (
  authAction: AuthAction,
  override val uuidService: UuidService,
  getRecordsConnector: GetRecordsRouterConnector,
  appConfig: AppConfig,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ValidationRules
    with Logging {

  def getRecord(eori: String, recordId: String): Action[AnyContent] =
    authAction(eori).async { implicit request =>
      val result = for {
        _               <- validateClientIdIfSupported //ToDO: remove this test after drop1.1 - TGP-1889
        _               <- EitherT
                             .fromEither[Future](validateAcceptHeader)
                             .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))
        serviceResponse <-
          EitherT(getRecordsConnector.get(eori, recordId)).leftMap(e => Status(e.status)(toJson(e.errorResponse)))
      } yield Ok(toJson(serviceResponse))

      result.merge
    }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): Action[AnyContent] =
    authAction(eori).async { implicit request =>
      val result = for {
        _               <- validateClientIdIfSupported //ToDO: remove this test after drop1.1 - TGP-1889
        _               <- EitherT
                             .fromEither[Future](validateAcceptHeader)
                             .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))
        serviceResponse <-
          EitherT(getRecordsConnector.get(eori, lastUpdatedDate, page, size)).leftMap(e =>
            Status(e.status)(toJson(e.errorResponse))
          )
      } yield Ok(toJson(serviceResponse))

      result.merge
    }

  /*
  ToDO: remove this test after drop1.1 - TGP-1889

  The client ID does not need to be checked anymore as EIS has removed it
  from the header
   */
  private def validateClientIdIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromEither[Future](
        if (!appConfig.isDrop1_1_enabled) validateClientIdHeader
        else Right("")
      )
      .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))

}

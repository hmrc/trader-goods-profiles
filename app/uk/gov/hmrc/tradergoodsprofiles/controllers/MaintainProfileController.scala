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
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.MaintainProfileRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, UserAllowListAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MaintainProfileController @Inject() (
  authAction: AuthAction,
  userAllowListAction: UserAllowListAction,
  maintainProfileRouterConnector: MaintainProfileRouterConnector,
  override val uuidService: UuidService,
  appConfig: AppConfig,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ValidationRules
    with Logging {

  def updateProfile(eori: String): Action[JsValue] =
    (authAction(eori) andThen userAllowListAction).async(parse.json) { implicit request =>
      val result = for {
        _               <- EitherT.fromEither[Future](validateAcceptAndContentTypeHeaders)
        _               <- validateClientIdIfSupported //ToDO: remove this test after eis impl - TGP-1889
        serviceResponse <-
          EitherT(maintainProfileRouterConnector.put(eori, request)).leftMap(e =>
            Status(e.status)(toJson(e.errorResponse))
          )
      } yield Ok(toJson(serviceResponse))

      result.merge
    }

  private def validateClientIdIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromEither[Future](
        if (appConfig.sendClientId) validateClientIdHeader
        else Right("")
      )
      .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))

}

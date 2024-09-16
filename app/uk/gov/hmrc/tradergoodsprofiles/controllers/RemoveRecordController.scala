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
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.RemoveRecordRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, UserAllowListAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveRecordController @Inject() (
  authAction: AuthAction,
  userAllowListAction: UserAllowListAction,
  removeRecordConnector: RemoveRecordRouterConnector,
  appConfig: AppConfig,
  override val uuidService: UuidService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ValidationRules {

  def removeRecord(eori: String, recordId: String, actorId: String): Action[AnyContent] =
    (authAction(eori) andThen userAllowListAction).async { implicit request =>
      val result = for {
        _ <- validateClientIdHeaderIfSupported
        _ <- validateAcceptHeaderIfSupported
        _ <- EitherT(removeRecordConnector.removeRecord(eori, recordId, actorId))
               .leftMap(e => Status(e.status)(toJson(e.errorResponse)))
      } yield NoContent

      result.merge
    }

  private def validateClientIdHeaderIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromEither[Future](
        if (appConfig.sendClientId) validateClientIdHeader
        else Right("")
      )
      .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))

  private def validateAcceptHeaderIfSupported(implicit request: Request[_]): EitherT[Future, Result, String] =
    EitherT
      .fromEither[Future](
        if (appConfig.sendAcceptHeader) validateAcceptHeader
        else Right("")
      )
      .leftMap(e => createBadRequestResponse(e.code, e.message, e.errorNumber))
}

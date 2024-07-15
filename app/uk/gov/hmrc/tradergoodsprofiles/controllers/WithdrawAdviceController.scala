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
import com.google.inject.Singleton
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.connectors.WithdrawAdviceRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.{AuthAction, ValidationRules}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WithdrawAdviceController @Inject() (
  authAction: AuthAction,
  withdrawAdviceRouterConnector: WithdrawAdviceRouterConnector,
  override val uuidService: UuidService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ValidationRules {

  def withdrawAdvice(eori: String, recordId: String): Action[JsValue] =
    authAction(eori).async(parse.json) { implicit request =>
      val result = for {
        _ <- EitherT.fromEither[Future](validateAcceptAndClientIdHeaders)
        _ <- EitherT(withdrawAdviceRouterConnector.withdrawAdvice(eori, recordId, request))
               .leftMap(e => Status(e.status)(toJson(e.errorResponse)))
      } yield NoContent

      result.merge
    }

}

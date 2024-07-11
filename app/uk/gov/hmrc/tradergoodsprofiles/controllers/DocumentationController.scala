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

import javax.inject.{Inject, Singleton}
import controllers.Assets
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.templates.txt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DocumentationController @Inject() (assets: Assets, cc: ControllerComponents, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) extends BackendController(cc) {

  def definition(): Action[AnyContent] =
    assets.at("/public/api", "definition.json")

  def specification(version: String, file: String): Action[AnyContent] =
    if (file == "application.yaml") {
      returnTemplatedYaml()
    } else {
      returnStaticAsset(version, file)
    }

  private def returnTemplatedYaml(): Action[AnyContent] = Action {
    val includeWithdrawAdviceEndpoint = appConfig.withdrawAdviceEnabled
    Ok(txt.application(includeWithdrawAdviceEndpoint)).as("application/yaml")
  }

  private def returnStaticAsset(version: String, file: String): Action[AnyContent] = Action.async { implicit request =>
    val path     = s"/public/api/conf/$version/$file"
    val resource = Option(getClass.getResource(path))
    resource match {
      case Some(_) => assets.at(path).apply(request)
      case None    => Future.successful(NotFound)
    }
  }
}

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

import controllers.Assets
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.templates.txt.ApiSchema

import javax.inject.{Inject, Singleton}

@Singleton
class DocumentationController @Inject() (
  assets: Assets,
  cc: ControllerComponents,
  appConfig: AppConfig,
  apiSpec: ApiSchema
) extends BackendController(cc)
    with Logging {
  def definition(): Action[AnyContent] =
    assets.at("/public/api", "definition.json")

  def specification(version: String, file: String): Action[AnyContent] =
    if ("application.yaml".equalsIgnoreCase(file)) {
      returnTemplatedYaml()
    } else {
      returnStaticAsset(version, file)
    }

  private def returnTemplatedYaml(): Action[AnyContent] = Action {
    logger.info(
      s"""Generating OpenAPI Spec with includeWithdrawAdviceEndpoint: ${appConfig.requestAdviceEnabled},
          includeWithdrawAdviceEndpoint: ${appConfig.withdrawAdviceEnabled},
          putMethodEnabled: ${appConfig.putMethodEnabled}"""
    )
    Ok(apiSpec()).as("application/yaml")
  }

  private def returnStaticAsset(version: String, file: String): Action[AnyContent] =
    assets.at(s"/public/api/conf/$version", file)
}

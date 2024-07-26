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

package uk.gov.hmrc.tradergoodsprofiles.config

import io.lemonlabs.uri.Url
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  lazy val appName: String = config.get[String]("appName")

  lazy val userAllowListBaseUrl: Url = Url.parse(servicesConfig.baseUrl("user-allow-list"))
  lazy val routerUrl: Url            = Url.parse(servicesConfig.baseUrl("trader-goods-profiles-router"))

  lazy val internalAuthToken: String = config.get[String]("internal-auth.token")

  lazy val withdrawAdviceEnabled: Boolean = config.get[Boolean]("features.withdrawAdviceEnabled")
  lazy val requestAdviceEnabled: Boolean  = config.get[Boolean]("features.requestAdviceEnabled")

  val isDrop1_1_enabled: Boolean =
    config
      .getOptional[Boolean]("features.drop_1_1_enabled")
      .getOrElse(false)
}

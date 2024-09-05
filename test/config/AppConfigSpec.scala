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

package config

import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig

class AppConfigSpec extends PlaySpec {

  private val validAppConfig =
    """
      |appName=trader-goods-profiles
      |feature.drop_1_1_enabled=true
    """.stripMargin

  private def createAppConfig(configSettings: String) = {
    val config        = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {

    "return false if isDrop2Enabled is missing" in {
      createAppConfig("").isDrop2Enabled mustBe false
    }

    "return false if isDrop2Enabled is false" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |feature.drop2Enabled=false
          |""".stripMargin
      createAppConfig(validAppConfig).isDrop2Enabled mustBe false
    }

    "return true if isDrop2Enabled is true" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |feature.drop2Enabled=true
          |""".stripMargin
      createAppConfig(validAppConfig).isDrop2Enabled mustBe true
    }

    "return true for clientIdHeaderDisabled when it is set to true" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |feature.clientIdHeaderDisabled=true
          |""".stripMargin
      createAppConfig(config).isClientIdHeaderDisabled mustBe true
    }

    "return false for clientIdHeaderDisabled when it is missing" in {
      createAppConfig("").isClientIdHeaderDisabled mustBe false
    }

    "return false for clientIdHeaderDisabled when it is set to false" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |feature.clientIdHeaderDisabled=false
          |""".stripMargin
      createAppConfig(config).isClientIdHeaderDisabled mustBe false
    }
  }
}

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
      |features.sendClientId=true
    """.stripMargin

  private def createAppConfig(configSettings: String) = {
    val config        = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {

    "return false if putMethodEnabled is missing" in {
      createAppConfig("").putMethodEnabled mustBe false
    }

    "return false if putMethodEnabled is false" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.putMethodEnabled=false
          |""".stripMargin
      createAppConfig(validAppConfig).putMethodEnabled mustBe false
    }

    "return true if putMethodEnabled is true" in {
      val validAppConfig =
        """
          |appName=trader-goods-profiles-router
          |features.putMethodEnabled=true
          |""".stripMargin
      createAppConfig(validAppConfig).putMethodEnabled mustBe true
    }

    "return true for sendClientId when it is set to true" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.sendClientId=true
          |""".stripMargin
      createAppConfig(config).sendClientId mustBe true
    }

    "return true for sendClientId when it is missing" in {
      createAppConfig("").sendClientId mustBe true
    }

    "return false for sendClientId when it is set to false" in {
      val config =
        """
          |appName=trader-goods-profiles-router
          |features.sendClientId=false
          |""".stripMargin
      createAppConfig(config).sendClientId mustBe false
    }
  }
}

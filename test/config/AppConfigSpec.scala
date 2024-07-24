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
    val config = ConfigFactory.parseString(configSettings)
    val configuration = Configuration(config)
    new AppConfig(configuration, new ServicesConfig(configuration))
  }

  val configService: AppConfig = createAppConfig(validAppConfig)

  "AppConfig" should {
    "return true for isDrop1_1_Enabled" in {
      configService.isDrop1_1_enabled mustBe true
    }

    "return false if isDrop1_1_Enabled is missing" in {
      createAppConfig("").isDrop1_1_enabled mustBe false
    }
  }
}

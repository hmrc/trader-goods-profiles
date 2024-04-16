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

package uk.gov.hmrc.tradergoodsprofiles

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class MicroserviceHelloWorldIntegrationSpec extends PlaySpec
  with GuiceOneServerPerSuite {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "Hello World" should {
    "return status OK" in {
      val result: WSResponse = await(wsClient.url(s"http://localhost:$port/hello-world").get)

      result.status mustBe OK
    }

    "return json content" in {
      val result: WSResponse = await(wsClient.url(s"http://localhost:$port/hello-world").get)

      result.body mustBe """Hello world"""
    }

    "return 401 for an unauthorised enrolment" in {
      val result: WSResponse = await(wsClient.url(s"http://localhost:$port/hello-world").get)

      result.status mustBe UNAUTHORIZED
    }
  }
}

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

package uk.gov.hmrc.tradergoodsprofiles.connectors

import io.lemonlabs.uri.Url
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofiles.connectors.UserAllowListConnector.UnexpectedResponseException
import uk.gov.hmrc.tradergoodsprofiles.support.BaseConnectorSpec

import scala.concurrent.Future

private class UserAllowListConnectorSpec
    extends BaseConnectorSpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach {

  private val baseUrl: Url = Url.parse("http://localhost:12345")
  private val connector    = new UserAllowListConnector(httpClient, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig)
    when(appConfig.userAllowListBaseUrl).thenReturn(baseUrl)
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
  }

  "check allow list" should {
    "return true when the given EORi is found" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(200, "")))

      val result = await(connector.check("private-beta", "12345"))

      result mustBe true
    }

    "return false when the given EORi is not found" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(404, "")))

      val result = await(connector.check("private-beta", "67890"))

      result mustBe false
    }

    "return a UnexpectedResponseException when user-allow-list throws an error" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse.apply(400, "")))

      assertThrows[UnexpectedResponseException](await(connector.check("private-beta", "12345")))
    }

  }
}

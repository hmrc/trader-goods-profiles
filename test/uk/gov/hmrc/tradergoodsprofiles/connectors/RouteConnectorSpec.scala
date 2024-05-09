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

import io.lemonlabs.uri.{Url, UrlPath}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.{AppConfig, Constants}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RouteConnectorSpec extends PlaySpec with ScalaFutures with EitherValues with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(Constants.XClientIdHeader -> "clientId"))

  private val httpClient     = mock[HttpClientV2]
  private val appConfig      = mock[AppConfig]
  private val requestBuilder = mock[RequestBuilder]
  private val timestamp      = Instant.parse("2024-05-12T12:15:15.456321Z")

  private val sut = new RouterConnector(httpClient, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig)
    when(appConfig.routerUrl).thenReturn(Url.parse("http://localhost:23123"))
    when(httpClient.get(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.execute).thenReturn(Future.successful(HttpResponse(200, "message")))
  }

  "get" should {
    "send a request with the right url" in {

      await(sut.get("eoriNumber", "recordId")(hc))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/eoriNumber/records/recordId")
      verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-Id"            -> "clientId")
      verify(requestBuilder).execute
    }

    "return 200" in {
      val result = await(sut.get("eoriNumber", "recordId"))

      result.status mustBe OK
    }

    "return 500 when httpClient throw" in {
      when(requestBuilder.execute).thenReturn(Future.failed(new RuntimeException("error")))

      val result = await(sut.get("eoriNumber", "recordId"))

      result.status mustBe INTERNAL_SERVER_ERROR
      result mustBe InternalServerError(
        Json.obj(
          "timestamp" -> "2024-05-12T12:15:15Z",
          "code"      -> "INTERNAL_SERVER_ERROR",
          "message"   -> "error"
        )
      )
    }
  }
}

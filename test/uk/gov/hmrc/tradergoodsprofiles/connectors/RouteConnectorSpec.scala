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

import com.codahale.metrics.{Counter, MetricRegistry, Timer}
import io.lemonlabs.uri.{Url, UrlPath}
import org.mockito.ArgumentMatchers.endsWith
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{CREATED, OK}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.{AppConfig, Constants}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{RouterCreateRecordRequestSupport, RouterUpdateRecordRequestSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.router.RouterUpdateProfileRequest

import scala.concurrent.{ExecutionContext, Future}

class RouteConnectorSpec
    extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach
    with RouterCreateRecordRequestSupport
    with RouterUpdateRecordRequestSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(Constants.XClientIdHeader -> "clientId"))

  private val httpClient                      = mock[HttpClientV2]
  private val appConfig                       = mock[AppConfig]
  private val requestBuilder                  = mock[RequestBuilder]
  private val timerContext                    = mock[Timer.Context]
  private val successCounter                  = mock[Counter]
  private val failureCounter                  = mock[Counter]
  private val metricsRegistry: MetricRegistry = mock[MetricRegistry](RETURNS_DEEP_STUBS)

  private val sut = new RouterConnector(httpClient, appConfig, metricsRegistry)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder, metricsRegistry, timerContext)

    when(appConfig.routerUrl).thenReturn(Url.parse("http://localhost:23123"))
    when(httpClient.get(any)(any)).thenReturn(requestBuilder)
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(httpClient.put(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.execute[HttpResponse](any, any))
      .thenReturn(Future.successful(HttpResponse(200, "message")))

    when(metricsRegistry.counter(endsWith("success-counter"))) thenReturn successCounter
    when(metricsRegistry.counter(endsWith("failed-counter"))) thenReturn failureCounter
    when(metricsRegistry.timer(any).time()) thenReturn timerContext
    when(timerContext.stop()) thenReturn 0L
  }

  "get single record" should {

    "return 200" in {
      val result = await(sut.get("eoriNumber", "recordId"))

      result.status mustBe OK
    }

    "send a request with the right url" in {

      await(sut.get("eoriNumber", "recordId"))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/eoriNumber/records/recordId")
      verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.getrecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }
  "get multiple records" should {

    "return 200" in {
      val result = await(sut.getRecords("eoriNumber"))

      result.status mustBe OK
    }

    "return 200 with optional query parameters" in {
      val result = await(sut.getRecords("eoriNumber", Some("2024-06-08T12:12:12.456789Z"), Some(1), Some(1)))

      result.status mustBe OK
    }

    "return 200 with optional query parameters page and size" in {
      val result = await(sut.getRecords("eoriNumber", None, Some(1), Some(1)))

      result.status mustBe OK
    }

    "send a request with the right url" in {

      await(sut.getRecords("eoriNumber"))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/eoriNumber")
      verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.getrecords.connector-timer"))
        verify(timerContext).stop()
      }
    }

    "send a request with the right url with optional query parameter" in {

      await(sut.getRecords("eoriNumber", Some("2024-06-08T12:12:12.456789Z"), Some(1), Some(1)))

      val expectedUrl =
        "http://localhost:23123/trader-goods-profiles-router/eoriNumber?lastUpdatedDate=2024-06-08T12:12:12.456789Z&page=1&size=1"

      verify(httpClient).get(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.getrecords.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "post" should {

    "return 201 when the record is successfully created" in {
      val createRecordRequest = createRouterCreateRecordRequest()

      when(httpClient.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      val result = await(sut.post(createRecordRequest))

      result.status mustBe CREATED
    }

    "send a request with the right url and body" in {
      val createRecordRequest = createRouterCreateRecordRequest()

      when(httpClient.post(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      await(sut.post(createRecordRequest))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/records")
      verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(Json.toJson(createRecordRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.createrecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "remove" should {

    "return 200" in {
      val result = await(sut.put("eoriNumber", "recordId", "actorId"))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {

      await(sut.put("eoriNumber", "recordId", "actorId"))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/eoriNumber/records/recordId")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(Json.obj("actorId" -> "actorId"))
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.removerecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "update" should {

    "return 200" in {
      val updateRecordRequest = createRouterUpdateRecordRequest()

      when(httpClient.put(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.put(updateRecordRequest))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {
      val updateRecordRequest = createRouterUpdateRecordRequest()

      when(httpClient.put(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.put(updateRecordRequest))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/records")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(Json.toJson(updateRecordRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.updaterecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "maintain profile" should {

    "return 200 when the profile is successfully updated" in {
      val routerUpdateProfileRequest = RouterUpdateProfileRequest(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(httpClient.put(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.routerMaintainProfile(routerUpdateProfileRequest))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {
      val routerUpdateProfileRequest = RouterUpdateProfileRequest(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(httpClient.put(any)(any)).thenReturn(requestBuilder)
      when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
      when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.routerMaintainProfile(routerUpdateProfileRequest))

      val expectedUrl =
        UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/profile/maintain")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).withBody(eqTo(Json.toJson(routerUpdateProfileRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.maintainprofile.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

}

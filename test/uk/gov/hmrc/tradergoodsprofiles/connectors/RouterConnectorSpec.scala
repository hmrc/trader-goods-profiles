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
import play.api.http.Status.{CREATED, NO_CONTENT, OK}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{RouterCreateRecordRequestSupport, UpdateRecordRequestSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.MaintainProfileRequest
import uk.gov.hmrc.tradergoodsprofiles.models.requests.router.RouterRequestAdviceRequest
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.XClientIdHeader

import scala.concurrent.{ExecutionContext, Future}

class RouterConnectorSpec
    extends PlaySpec
    with ScalaFutures
    with EitherValues
    with BeforeAndAfterEach
    with RouterCreateRecordRequestSupport
    with UpdateRecordRequestSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = Seq(XClientIdHeader -> "clientId"))

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
    when(httpClient.delete(any)(any)).thenReturn(requestBuilder)
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

  "create" should {

    "return 201 when the record is successfully created" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      val result = await(sut.createRecord("eori", createRouterCreateRecordRequest))

      result.status mustBe CREATED
    }

    "send a request with the right url and body" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      await(sut.createRecord("eoriNumber", createRouterCreateRecordRequest))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/traders/eoriNumber/records")
      verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(Json.toJson(createRouterCreateRecordRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.createrecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "remove" should {

    "return 204" in {
      when(requestBuilder.execute[HttpResponse](any, any))
        .thenReturn(Future.successful(HttpResponse(204, "")))

      val result = await(sut.removeRecord("eoriNumber", "recordId", "actorId"))

      result.status mustBe NO_CONTENT
    }

    "send a DELETE request with the right url and body" in {

      await(sut.removeRecord("eoriNumber", "recordId", "actorId"))

      val expectedUrl =
        "http://localhost:23123/trader-goods-profiles-router/traders/eoriNumber/records/recordId?actorId=actorId"
      verify(httpClient).delete(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader("X-Client-ID" -> "clientId")
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.removerecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "update" should {

    val eoriNumber = "GB123456789012"
    val recordId   = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

    "return 200" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.updateRecord(eoriNumber, recordId, createUpdateRecordRequest))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.updateRecord(eoriNumber, recordId, createUpdateRecordRequest))

      val expectedUrl =
        UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(Json.toJson(createUpdateRecordRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.updaterecord.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  "request advice" should {

    "return 201 when advice is successfully requested" in {
      val requestAdviceRequest = createRouterRequestAdviceRequest()

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "")))

      val result = await(sut.requestAdvice(requestAdviceRequest))

      result.status mustBe CREATED
    }

    "send a request with the right url and body" in {
      val requestAdviceRequest = createRouterRequestAdviceRequest()

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      await(sut.requestAdvice(requestAdviceRequest))

      val expectedUrl = UrlPath.parse("http://localhost:23123/trader-goods-profiles-router/createaccreditation")
      verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(Json.toJson(requestAdviceRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.requestadvice.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

  def createRouterRequestAdviceRequest(): RouterRequestAdviceRequest = RouterRequestAdviceRequest(
    eori = "GB987654321098",
    requestorName = "Mr.Phil Edwards",
    recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    requestorEmail = "Phil.Edwards@gmail.com"
  )

  "maintain profile" should {

    "return 200 when the profile is successfully updated" in {
      val eori = "GB123456789012"

      val updateProfileRequest = MaintainProfileRequest(
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.routerMaintainProfile(eori, updateProfileRequest))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {
      val eori = "GB123456789012"

      val updateProfileRequest = MaintainProfileRequest(
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.routerMaintainProfile(eori, updateProfileRequest))

      val expectedUrl =
        UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eori")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).withBody(eqTo(Json.toJson(updateProfileRequest)))(any, any, any)
      verify(requestBuilder).execute(any, any)

      withClue("process the response within a timer") {
        verify(metricsRegistry).timer(eqTo("tgp.maintainprofile.connector-timer"))
        verify(timerContext).stop()
      }
    }
  }

}

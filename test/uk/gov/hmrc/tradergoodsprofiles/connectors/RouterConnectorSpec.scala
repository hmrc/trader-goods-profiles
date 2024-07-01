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
import play.api.http.Status.{CREATED, OK}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{RouterCreateRecordRequestSupport, UpdateRecordRequestSupport}
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

  private val httpClient     = mock[HttpClientV2]
  private val appConfig      = mock[AppConfig]
  private val requestBuilder = mock[RequestBuilder]
  private val eori           = "GB123456789012"
  private val recordId       = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val sut = new RouterConnector(httpClient, appConfig)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(httpClient, appConfig, requestBuilder)

    when(appConfig.routerUrl).thenReturn(Url.parse("http://localhost:23123"))
    when(httpClient.get(any)(any)).thenReturn(requestBuilder)
    when(httpClient.post(any)(any)).thenReturn(requestBuilder)
    when(httpClient.put(any)(any)).thenReturn(requestBuilder)
    when(httpClient.patch(any)(any)).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any)).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any))
      .thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[Object])(any, any, any)).thenReturn(requestBuilder)
    when(requestBuilder.execute[HttpResponse](any, any))
      .thenReturn(Future.successful(HttpResponse(200, "message")))
  }

  "update" should {

    "return 200" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.updateRecord(eori, recordId, createUpdateRecordRequest))

      result.status mustBe OK
    }

    "send a PATCH request with the right url and body" in {
      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.updateRecord(eori, recordId, createUpdateRecordRequest))

      val expectedUrl =
        UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eori/records/$recordId")
      verify(httpClient).patch(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(createUpdateRecordRequestData))(any, any, any)
      verify(requestBuilder).execute(any, any)
    }
  }

  "request advice" should {

    "return 201 when advice is successfully requested" in {

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "")))

      val result = await(sut.requestAdvice(createRouterRequestAdviceRequest, eori, recordId))

      result.status mustBe CREATED
    }

    "send a request with the right url and body" in {

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(201, "message")))

      await(sut.requestAdvice(createRouterRequestAdviceRequest, eori, recordId))

      val expectedUrl =
        UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eori/records/$recordId/advice")
      verify(httpClient).post(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).setHeader("X-Client-ID"            -> "clientId")
      verify(requestBuilder).withBody(eqTo(requestAdviceData))(any, any, any)
      verify(requestBuilder).execute(any, any)
    }
  }

  def requestAdviceData: JsValue = Json
    .parse("""
             |{
             |    "requestorName": "Mr.Phil Edwards",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |
             |}
             |""".stripMargin)

  def requestAdviceJsonRequest: Request[JsValue]         =
    FakeRequest().withBody(requestAdviceData)
  val createRouterRequestAdviceRequest: Request[JsValue] = requestAdviceJsonRequest

  "maintain profile" should {

    "return 200 when the profile is successfully updated" in {
      val eori = "GB123456789012"

      def updateProfileRequestData: JsValue = Json
        .parse("""
                 |{
                 |    "actorId": "GB987654321098",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345"
                 |
                 |}
                 |""".stripMargin)

      def updateProfileJson: Request[JsValue]    =
        FakeRequest().withBody(updateProfileRequestData)
      val updateProfileRequest: Request[JsValue] = updateProfileJson

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      val result = await(sut.routerMaintainProfile(eori, updateProfileRequest))

      result.status mustBe OK
    }

    "send a PUT request with the right url and body" in {
      val eori = "GB123456789012"

      def updateProfileRequestData: JsValue = Json
        .parse("""
                 |{
                 |    "actorId": "GB987654321098",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345"
                 |
                 |}
                 |""".stripMargin)

      def updateProfileJson: Request[JsValue]    =
        FakeRequest().withBody(updateProfileRequestData)
      val updateProfileRequest: Request[JsValue] = updateProfileJson

      when(requestBuilder.execute[HttpResponse](any, any)).thenReturn(Future.successful(HttpResponse(200, "message")))

      await(sut.routerMaintainProfile(eori, updateProfileRequest))

      val expectedUrl =
        UrlPath.parse(s"http://localhost:23123/trader-goods-profiles-router/traders/$eori")
      verify(httpClient).put(eqTo(url"$expectedUrl"))(any)
      verify(requestBuilder).setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      verify(requestBuilder).withBody(eqTo(updateProfileRequestData))(any, any, any)
      verify(requestBuilder).execute(any, any)
    }
  }

}

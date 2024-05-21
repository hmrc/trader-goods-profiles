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

package uk.gov.hmrc.tradergoodsprofiles.controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.APICreateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class CreateRecordControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with CreateRecordResponseSupport
    with APICreateRecordRequestSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val dateTimeService    = mock[DateTimeService]
  private lazy val timestamp          = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString

  private val url              = s"http://localhost:$port/$eoriNumber/records"
  private val routerUrl        = s"/trader-goods-profiles-router/records"
  private val requestBody      = Json.toJson(createAPICreateRecordRequest())
  private val expectedResponse = Json.toJson(createCreateRecordResponse(recordId, eoriNumber, timestamp))

  override lazy val app: Application = {
    wireMock.start()
    configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServices)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[DateTimeService].to(dateTimeService),
        bind[HttpClientV2].to(httpClientV2)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)
    stubRouterRequest(CREATED, expectedResponse.toString())
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "CreateRecordController" should {
    "successfully create a record and return 201" in {
      withAuthorizedTrader()

      val result = createRecordAndWait()

      result.status mustBe CREATED
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        verify(
          postRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return BadRequest for invalid request body" in {
      withAuthorizedTrader()
      val invalidRequestBody = Json.obj()

      val result = createRecordAndWait(invalidRequestBody)

      result.status mustBe BAD_REQUEST
      result.json mustBe Json.obj(
        "code"    -> "INVALID JSON",
        "message" -> Json.obj(
          "obj.comcode"                  -> "error.path.missing",
          "obj.comcodeEffectiveFromDate" -> "error.path.missing",
          "obj.actorId"                  -> "error.path.missing",
          "obj.traderRef"                -> "error.path.missing",
          "obj.goodsDescription"         -> "error.path.missing",
          "obj.category"                 -> "error.path.missing",
          "obj.countryOfOrigin"          -> "error.path.missing"
        )
      )
    }

    "return Forbidden when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = createRecordAndWaitWithoutClientIdHeader()

      result.status mustBe FORBIDDEN
      result.json mustBe Json.obj(
        "timestamp" -> "2024-06-08T12:12:12Z",
        "code"      -> "INVALID_HEADER_PARAMETERS",
        "message"   -> "X-Client-ID header is missing"
      )
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = createRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe Json.obj(
        "timestamp" -> "2024-06-08T12:12:12Z",
        "code"      -> "FORBIDDEN",
        "message"   -> "This EORI number is incorrect"
      )
    }

  }

  private def createRecordAndWaitWithoutClientIdHeader() =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .post(requestBody)
    )

  private def createRecordAndWait(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "clientId",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .post(requestBody)
    )

  private def stubRouterRequest(status: Int, responseBody: String) =
    wireMock.stubFor(
      post(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )
}

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
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.requests.RequestAdviceRequest
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.util.UUID
import scala.concurrent.ExecutionContext

class RequestAdviceIntegrationTest
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url         = s"http://localhost:$port/$eoriNumber/records/$recordId/advice"
  private val routerUrl   = s"/trader-goods-profiles-router/createaccreditation"
  private val requestBody = Json.toJson(createRequestAdviceRequest())

  override lazy val app: Application = {
    wireMock.start()
    configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServices)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[UuidService].to(uuidService),
        bind[HttpClientV2].to(httpClientV2)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)
    wireMock.stubFor(
      post(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(CREATED)
        )
    )
    when(uuidService.uuid).thenReturn(correlationId)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "Request Advice" should {
    "successfully request advice and return 201" in {
      withAuthorizedTrader()

      val result = requestAdviceAndWait()

      result.status mustBe CREATED

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

      val result = requestAdviceAndWait(invalidRequestBody)

      result.status mustBe BAD_REQUEST
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Seq(
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> s"Mandatory field requestorEmail was missing from body or is in the wrong format",
            "errorNumber" -> 1009
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> s"Mandatory field requestorName was missing from body or is in the wrong format",
            "errorNumber" -> 1008
          ),
          Json.obj(
            "code"        -> "INVALID_REQUEST_PARAMETER",
            "message"     -> s"Mandatory field actorId was missing from body or is in the wrong format",
            "errorNumber" -> 8
          )
        )
      )

    }

    "return Bad Request when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = requestAdviceAndWaitWithoutClientIdHeader()

      result.status mustBe BAD_REQUEST
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Seq(
          Json.obj(
            "code"        -> "INVALID_HEADER_PARAMETER",
            "message"     -> "X-Client-ID was missing from Header or is in wrong format",
            "errorNumber" -> 6000
          )
        )
      )
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = requestAdviceAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe expectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = requestAdviceAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe expectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = requestAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe expectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = requestAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe expectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = requestAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe expectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Internal server error if auth throws" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = requestAdviceAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe expectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId/advice with error: runtime exception"
      )
    }

  }

  def createRequestAdviceRequest(): RequestAdviceRequest = RequestAdviceRequest(
    actorId = "XI123456789001",
    requestorName = "Mr.Phil Edwards",
    requestorEmail = "Phil.Edwards@gmail.com"
  )

  private def requestAdviceAndWaitWithoutClientIdHeader() =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .post(requestBody)
    )

  private def requestAdviceAndWait(requestBody: JsValue = requestBody) =
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

  private def expectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
    )
}

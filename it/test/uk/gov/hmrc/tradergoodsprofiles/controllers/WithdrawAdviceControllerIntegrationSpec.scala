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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, configureFor, equalTo, urlEqualTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, writeableOf_JsValue}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.{JsonHelper, WireMockServerSpec}

import java.util.UUID
import scala.concurrent.ExecutionContext

class WithdrawAdviceControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll
      with JsonHelper {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]

  private val url         = s"http://localhost:$port/$eoriNumber/records/$recordId/advice"
  private val routerUrl   =
    s"/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId/advice"
  private val requestBody = createWithdrawAdviceRequest

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
    stubForUserAllowList
    stubRouterResponse(NO_CONTENT, "204")
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

  "Withdraw Advice" should {
    "successfully withdraw advice and return 204" in {
      withAuthorizedTrader()

      val result = withdrawAdviceAndWait()

      result.status mustBe NO_CONTENT

      withClue("should add the right headers") {
        WireMock.verify(
          WireMock.putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return Bad Request when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = withdrawAdviceAndWaitWithoutClientIdHeader()

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

    "router return Bad Request" in {
      stubRouterResponse(BAD_REQUEST, routerError.toString())
      withAuthorizedTrader()
      val result = withdrawAdviceAndWait()
      result.status mustBe BAD_REQUEST
      result.json mustBe routerError
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = withdrawAdviceAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect",
        Some("103")
      )
    }

    "return Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = withdrawAdviceAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect",
        Some("103")
      )
    }

    "return Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = withdrawAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile",
        Some("101")
      )
    }

    "return Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = withdrawAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'", Some("102")
      )
    }

    "return Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = withdrawAdviceAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'",
        Some("102")
      )
    }

    "return Internal server error if auth throws" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = withdrawAdviceAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId/advice with error: runtime exception"
      )
    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = withdrawAdviceAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }

  }

  private val routerError = Json.obj(
    "correlationId" -> correlationId,
    "code"          -> "BAD_REQUEST",
    "message"       -> "Bad Request",
    "errors"        -> Json.arr(
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "X-Correlation-ID was missing from Header or is in the wrong format",
        "errorNumber" -> 1
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "X-Forwarded-Host was missing from Header os is in the wrong format",
        "errorNumber" -> 5
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Content-Type was missing from Header or is in the wrong format",
        "errorNumber" -> 3
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Accept was missing from Header or is in the wrong format",
        "errorNumber" -> 4
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Mandatory withdrawDate was missing from body",
        "errorNumber" -> 1013
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Mandatory goodsItems was missing from body",
        "errorNumber" -> 1014
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "The recordId has been provided in the wrong format",
        "errorNumber" -> 1017
      )
    )
  )

  private def withdrawAdviceAndWaitWithoutClientIdHeader() =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept" -> "application/vnd.hmrc.1.0+json"
        )
        .put(requestBody)
    )

  private def withdrawAdviceAndWait(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "clientId",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .put(requestBody)
    )

  private def stubRouterResponse(status: Int, errorResponse: String, url: String = routerUrl) =
    wireMock.stubFor(
      WireMock
        .put(url)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(errorResponse)
        )
    )

  private def createWithdrawAdviceRequest: JsValue = Json
    .parse("""
             |{
             |    "withdrawReason": "text"
             |}
             |""".stripMargin)
}

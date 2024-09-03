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
import io.lemonlabs.uri.Url
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
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import scala.concurrent.ExecutionContext

class MaintainProfileControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url       = s"http://localhost:$port/$eoriNumber"
  private val routerUrl = s"/trader-goods-profiles-router/traders/$eoriNumber"

  def updateProfileRequest: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB987654321098",
             |    "ukimsNumber": "XIUKIM47699357400020231115081800",
             |    "nirmsNumber": "RMS-GB-123456",
             |    "niphlNumber": "6 S12345"
             |
             |}
             |""".stripMargin)

  private val updateProfileResponse = MaintainProfileResponse(
    eoriNumber,
    "GB987654321098",
    Some("XIUKIM47699357400020231115081800"),
    Some("RMS-GB-123456"),
    Some("6 S12345")
  )

  private val requestBody      = updateProfileRequest
  private val expectedResponse = Json.toJson(updateProfileResponse)

  lazy private val appConfig = mock[AppConfig]

  override lazy val app: Application = {
    wireMock.start()
    configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .configure(configureServices)
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[UuidService].to(uuidService),
        bind[HttpClientV2].to(httpClientV2),
        bind[AppConfig].to(appConfig)
      )
      .build()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)
    stubForUserAllowList
    stubRouterRequest(OK, expectedResponse.toString())
    when(uuidService.uuid).thenReturn(correlationId)
    when(appConfig.routerUrl).thenReturn(Url.parse(wireMock.baseUrl))
    when(appConfig.userAllowListEnabled).thenReturn(true)
    when(appConfig.userAllowListBaseUrl).thenReturn(Url.parse(wireMock.baseUrl))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMock.resetAll()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    wireMock.stop()
  }

  "MaintainProfileController" should {
    // TODO: After the drop 1.1 create single test without the client id and remove the feature flag - TGP-2014
    "return 200 OK when the profile update is successful" in {
      withAuthorizedTrader()
      when(appConfig.isClientIdOptional).thenReturn(false)

      val result = updateProfileAndWait()

      println(s"Response: ${result.status}, Body: ${result.body}")

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        verify(
          putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
            .withHeader("X-Client-ID", equalTo("Some client Id"))
        )
      }
    }
    // TODO: After the drop 1.1 create single test without the client id and remove the feature flag - TGP-2014
    "return 200 OK without validating x-client-id when isClientIdOptional is true" in {
      withAuthorizedTrader()
      when(appConfig.isClientIdOptional).thenReturn(true)

      val result = updateProfileAndWaitWithoutClientId()

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        verify(
          putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
        )
      }
    }

    "return 400 Bad Request when Accept header is invalid" in {
      withAuthorizedTrader()

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Content-Type" -> "application/json"
          )
          .put(requestBody)
      )

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return 415 Unsupported Media Type when Content-Type header is empty or invalid" in {
      withAuthorizedTrader()

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Accept"       -> "application/vnd.hmrc.1.0+json",
            "Content-Type" -> ""
          )
          .put(requestBody)
      )

      result.status mustBe UNSUPPORTED_MEDIA_TYPE
      result.json mustBe Json.obj(
        "statusCode" -> 415,
        "message"    -> "Expecting text/json or application/json body"
      )
    }

    "return 400 Bad Request when multiple mandatory fields are missing from body" in {
      stubRouterRequest(BAD_REQUEST, routerError.toString())
      withAuthorizedTrader()
      val invalidJson = Json.parse("""{"invalid": "json"}""")

      val result = updateProfileAndWait(invalidJson)

      result.status mustBe BAD_REQUEST
      result.json mustBe routerError
    }

    "return 403 Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = updateProfileAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe updateProfileExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return 403 Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = updateProfileAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe updateProfileExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return 401 Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = updateProfileAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateProfileExpectedJson(
        "UNAUTHORIZED",
        "The details signed in do not have a Trader Goods Profile"
      )
    }

    "return 401 Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = updateProfileAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateProfileExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return 401 Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = updateProfileAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateProfileExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return 500 Internal Server Error when the service layer fails" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = updateProfileAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe updateProfileExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber with error: runtime exception"
      )
    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = updateProfileAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe updateProfileExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }
  }

  // TODO: After the drop 1.1 create single method without the client id and remove the feature flag - TGP-2014
  private def updateProfileAndWait(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "Some client Id",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .put(requestBody)
    )
  // TODO: After the drop 1.1 create single method without the client id and remove the feature flag - TGP-2014

  private def updateProfileAndWaitWithoutClientId(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .put(requestBody)
    )

  private def stubRouterRequest(status: Int, responseBody: String) =
    wireMock.stubFor(
      put(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

  private def createExpectedError(code: String, message: String, errorNumber: Int): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> "BAD_REQUEST",
      "message"       -> "Bad Request",
      "errors"        -> Seq(
        Json.obj(
          "code"        -> code,
          "message"     -> message,
          "errorNumber" -> errorNumber
        )
      )
    )

  private def updateProfileExpectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
    )

  private val routerError = Json.obj(
    "correlationId" -> correlationId,
    "code"          -> "BAD_REQUEST",
    "message"       -> "Bad Request",
    "errors"        -> Json.arr(
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Mandatory field actorId was missing from body or is in the wrong format",
        "errorNumber" -> 8
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Mandatory field ukimsNumber was missing from body or is in the wrong format",
        "errorNumber" -> 33
      )
    )
  )

}

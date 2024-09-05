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
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, GetRecordsResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.response.GetRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.util.UUID
import scala.reflect.runtime.universe.typeOf

class GetRecordsControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with GetRecordResponseSupport
    with GetRecordsResponseSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val timestamp          = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val getSingleRecordUrl               = s"http://localhost:$port/$eoriNumber/records/$recordId"
  private val getMultipleRecordsUrl            = s"http://localhost:$port/$eoriNumber/records"
  private val getSingleRecordRouterUrl         = s"/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId"
  private val getMultipleRecordsRouterUrl      = s"/trader-goods-profiles-router/traders/$eoriNumber/records"
  private val getSingleRecordRouterResponse    = Json.toJson(createGetRecordResponse(eoriNumber, recordId, timestamp))
  private val getMultipleRecordsRouterResponse = Json.toJson(createGetRecordsResponse(eoriNumber))
  private val getMultipleRecordsCallerResponse =
    Json.toJson(createGetRecordsCallerResponse(eoriNumber))

  lazy private val appConfig = mock[AppConfig]

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

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
    stubRouterRequest(getMultipleRecordsRouterUrl, 200, getMultipleRecordsRouterResponse.toString())
    stubRouterRequest(getSingleRecordRouterUrl, 200, getSingleRecordRouterResponse.toString())
    when(uuidService.uuid).thenReturn(correlationId)
    when(appConfig.isClientIdHeaderDisabled).thenReturn(false)
    when(appConfig.userAllowListEnabled).thenReturn(true)
    when(appConfig.routerUrl).thenReturn(Url.parse(wireMock.baseUrl))
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

  "GET single record" should {
    "return a record" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe OK
      result.json mustBe getSingleRecordRouterResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(urlEqualTo(getSingleRecordRouterUrl))
        )
      }
    }

    "should not validate client ID is feature flag isClientIdHeaderDisabled is true" in {
      withAuthorizedTrader()
      when(appConfig.isClientIdHeaderDisabled).thenReturn(true)

      val result = getRecordAndWaitWithOutClientIDHeader(getSingleRecordUrl)

      result.status mustBe OK
      result.json mustBe getSingleRecordRouterResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(urlEqualTo(getSingleRecordRouterUrl))
        )
      }
    }

    "return a 404 if the router returns a 404" in {
      withAuthorizedTrader()
      val routerResponse = Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found",
        "errors"        -> null
      )

      val expectedErrorResponse = Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found"
      )

      stubRouterRequest(getSingleRecordRouterUrl, 404, routerResponse.toString())

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe NOT_FOUND
      result.json mustBe expectedErrorResponse

    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe OK
    }

    "return bad request when Accept header is invalid" in {
      withAuthorizedTrader()

      val result = await(
        wsClient
          .url(getSingleRecordUrl)
          .withHttpHeaders("X-Client-ID" -> "TSS")
          .get()
      )

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return internal server error if auth throw" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId with error: runtime exception"
      )
    }

    "return an BAD_REQUEST (400) from router if recordId is invalid" in {
      wireMock.stubFor(
        get(s"/trader-goods-profiles-router/traders/$eoriNumber/records/abcdfg-12gt")
          .willReturn(
            aResponse()
              .withStatus(400)
              .withBody(
                createExpectedError(
                  "INVALID_REQUEST_PARAMETER",
                  "The recordId has been provided in the wrong format",
                  25
                ).toString
              )
          )
      )

      withAuthorizedTrader()

      val result = getRecordAndWait(s"http://localhost:$port/$eoriNumber/records/abcdfg-12gt")

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_REQUEST_PARAMETER",
        "The recordId has been provided in the wrong format",
        25
      )
    }

    "return an error if API return an error with non json message" in {
      withAuthorizedTrader()
      stubRouterRequest(getSingleRecordRouterUrl, 404, "error")

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Response body could not be parsed as JSON, body: error"
      )

    }

    "return an internal server error if the router returns an error with null errors field" in {
      withAuthorizedTrader()

      val routerResponse =
        s"""
           |{
           |    "correlationId": "$correlationId",
           |    "code": "INTERNAL_SERVER_ERROR",
           |    "message": "Internal Server Error"
           |}
        """.stripMargin

      stubRouterRequest(getSingleRecordRouterUrl, 500, routerResponse)

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal Server Error"
      )

    }

    "return an error if json cannot be deserialized to the router message" in {
      withAuthorizedTrader()
      stubRouterRequest(getSingleRecordRouterUrl, 200, "{}")

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> s"Response body could not be read as type ${typeOf[GetRecordResponse]}"
      )
    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = getRecordAndWait(getSingleRecordUrl)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }
  }

  "GET multiple records" should {
    "return multiple records" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe OK
      result.json mustBe getMultipleRecordsCallerResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(urlEqualTo(getMultipleRecordsRouterUrl))
        )
      }
    }

    /* TGP-1889
    ToDo: remove this test after drop1.1 and refactor getRecordAndWait
    to remove the client ID header
     */
    "should not validate client ID is feature flag isClientIdHeaderDisabled is true" in {
      withAuthorizedTrader()
      when(appConfig.isClientIdHeaderDisabled).thenReturn(true)

      val result = getRecordAndWaitWithOutClientIDHeader(getMultipleRecordsUrl)

      result.status mustBe OK
      result.json mustBe getMultipleRecordsCallerResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(urlEqualTo(getMultipleRecordsRouterUrl))
        )
      }
    }
    "return multiple records with optional query parameters" in {
      withAuthorizedTrader()
      wireMock.stubFor(
        WireMock
          .get(s"$getMultipleRecordsRouterUrl?lastUpdatedDate=2024-06-08T12:12:12Z&page=1&size=1")
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(getMultipleRecordsRouterResponse.toString())
          )
      )

      val result =
        await(
          wsClient
            .url(
              s"http://localhost:$port/$eoriNumber/records?lastUpdatedDate=2024-06-08T12:12:12Z&page=1&size=1"
            )
            .withHttpHeaders(
              "Accept"       -> "application/vnd.hmrc.1.0+json",
              "Content-Type" -> "application/json",
              "X-Client-ID"  -> "TSS"
            )
            .get()
        )

      result.status mustBe OK
      result.json mustBe getMultipleRecordsCallerResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(
            urlEqualTo(s"$getMultipleRecordsRouterUrl?lastUpdatedDate=2024-06-08T12:12:12Z&page=1&size=1")
          )
        )
      }
    }

    "return an error if the router returns an error" in {
      withAuthorizedTrader()
      val routerResponse = Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found",
        "errors"        -> null
      )

      val expectedErrorResponse = Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found"
      )

      stubRouterRequest(getMultipleRecordsRouterUrl, 404, routerResponse.toString())

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe NOT_FOUND
      result.json mustBe expectedErrorResponse
    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe OK
    }

    "return Unauthorised when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(s"http://localhost:$port/wrongEoriNumber/records")

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return bad request when Accept header is invalid" in {
      withAuthorizedTrader()

      val result = await(
        wsClient
          .url(getMultipleRecordsUrl)
          .withHttpHeaders("X-Client-ID" -> "TSS")
          .get()
      )

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return internal server error if auth throw" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records with error: runtime exception"
      )
    }

    "return an error if API return an error with non json message" in {
      withAuthorizedTrader()
      stubRouterRequest(getMultipleRecordsRouterUrl, 404, "error")

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Response body could not be parsed as JSON, body: error"
      )

    }

    "return an error if json cannot be deserialized to the router message" in {
      withAuthorizedTrader()
      stubRouterRequest(getMultipleRecordsRouterUrl, 200, "{test}")

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> s"Response body could not be parsed as JSON, body: {test}"
      )
    }

    "return forbidden when EORI is not on the user allow list when getting multiple records" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = getRecordAndWait(getMultipleRecordsUrl)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }
  }

  /* TGP-1889
   ToDo: remove the X-Client-ID header after drop1.1
   */
  private def getRecordAndWait(url: String) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept"      -> "application/vnd.hmrc.1.0+json",
          "X-Client-ID" -> "TSS"
        )
        .get()
    )

  /* TGP-1889
   ToDo: after drop1.1 and refactory of the getRecordAndWait function,
   this function can be removed
   */
  private def getRecordAndWaitWithOutClientIDHeader(url: String) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept" -> "application/vnd.hmrc.1.0+json"
        )
        .get()
    )

  private def createExpectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
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

  private def stubRouterRequest(url: String, status: Int, response: String) =
    wireMock.stubFor(
      WireMock
        .get(url)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(response)
        )
    )
}

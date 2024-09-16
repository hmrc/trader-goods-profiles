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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.util.UUID

class RemoveRecordControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private val recordId                = UUID.randomUUID().toString
  private val actorId                 = "GB987654321098"
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url            = s"http://localhost:$port/$eoriNumber/records/$recordId?actorId=$actorId"
  private val routerUrl      = s"/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId?actorId=$actorId"
  private val routerResponse = NO_CONTENT

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
    stubRouterResponse(NO_CONTENT, routerResponse.toString)
    when(uuidService.uuid).thenReturn(correlationId)
    when(appConfig.sendClientId).thenReturn(true)
    when(appConfig.sendAcceptHeader).thenReturn(true)
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

  "remove record" should {
    "return 204 with the headers" in {
      withAuthorizedTrader()

      val result = removeRecordAndWait()

      result.status mustBe NO_CONTENT

      withClue("should add the right headers") {
        verify(
          deleteRequestedFor(urlEqualTo(routerUrl))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return an error if the router returns an error" in {
      withAuthorizedTrader()
      val routerResponse = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found"
      )

      val expectedErrorResponse = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found"
      )

      stubRouterResponse(404, routerResponse.toString())

      val result = removeRecordAndWait()

      result.status mustBe NOT_FOUND
      result.json mustBe expectedErrorResponse
    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = removeRecordAndWait()

      result.status mustBe NO_CONTENT
    }

    "return Unauthorised when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = removeRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = removeRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = removeRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = removeRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader()

      val result = removeRecordAndWait(s"http://localhost:$port/wrongEoriNumber/records/$recordId?actorId=$actorId")

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    //ToDo: remove for drop2 - TGP-2029
    "return bad request when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val headers = Seq("Accept" -> "application/vnd.hmrc.1.0+json")
      val result  = removeRecordAndWait(url, headers: _*)

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    //ToDo: remove for drop2 - TGP-2029
    "return bad request when Accept header is invalid" in {
      withAuthorizedTrader()

      val headers = Seq("X-Client-ID" -> "clientId", "Content-Type" -> "application/json")
      val result  = removeRecordAndWait(url, headers: _*)

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return internal server error if auth throw" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = removeRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId?actorId=$actorId with error: runtime exception"
      )
    }

    "return an error if API return an error with non json message" in {
      withAuthorizedTrader()
      stubRouterResponse(404, "error")

      val result = removeRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Response body could not be parsed as JSON, body: error"
      )

    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = removeRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }

  }

  private def removeRecordAndWait(url: String = url) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID" -> "clientId",
          "Accept"      -> "application/vnd.hmrc.1.0+json"
        )
        .delete()
    )

  private def removeRecordAndWait(url: String, headers: (String, String)*) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .delete()
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

  private def createExpectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
    )

  private def stubRouterResponse(status: Int, errorResponse: String, url: String = routerUrl) =
    wireMock.stubFor(
      WireMock
        .delete(url)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(errorResponse)
        )
    )

}

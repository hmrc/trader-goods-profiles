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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, GetRecordResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.GetRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.reflect.runtime.universe.typeOf

class GetRecordsControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with GetRecordResponseSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val dateTimeService    = mock[DateTimeService]
  private lazy val timestamp          = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString

  private val url            = s"http://localhost:$port/$eoriNumber/records/$recordId"
  private val routerUrl      = s"/trader-goods-profiles-router/$eoriNumber/records/$recordId"
  private val routerResponse = Json.toJson(createGetRecordResponse(eoriNumber, recordId, timestamp))

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

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
    stubRouterRequest(200, routerResponse.toString())
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

  "GET record" should {
    "return 200" in {
      withAuthorizedTrader()

      val result = getRecordAndWait()

      result.status mustBe OK
    }

    "return a record" in {
      withAuthorizedTrader()

      val result = getRecordAndWait()

      result.json mustBe routerResponse

      withClue("should add the right headers") {
        verify(
          getRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return an error if router return an error" in {
      withAuthorizedTrader()
      val routerResponse = Json.obj(
        "correlationId" -> "correlationId",
        "code"          -> "NOT_FOUND",
        "message"       -> "Not found"
      )

      stubRouterRequest(404, routerResponse.toString())

      val result = getRecordAndWait()

      result.status mustBe NOT_FOUND
      result.json mustBe routerResponse + ("timestamp" -> Json.toJson(timestamp.truncatedTo(ChronoUnit.SECONDS)))

    }
    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = getRecordAndWait()

      result.status mustBe OK
    }

    "return Unauthorised when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = getRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = getRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = getRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = getRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This EORI number is incorrect"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(s"http://localhost:$port/wrongEoriNumber/records/$recordId")

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This EORI number is incorrect"
      )
    }

    "return forbidden when Accept header is invalid" in {
      withAuthorizedTrader()

      val headers = Seq("X-Client-ID" -> "clientId", "Content-Type" -> "application/json")
      val result  = getRecordAndWait(url, headers: _*)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson("INVALID_HEADER_PARAMETERS", "Accept header is missing or invalid")
    }

    "return forbidden when Content-Type header is missing" in {
      withAuthorizedTrader()

      val headers = Seq("X-Client-ID" -> "clientId", "Accept" -> "application/vnd.hmrc.1.0+json")
      val result  = getRecordAndWait(url, headers: _*)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "INVALID_HEADER_PARAMETERS",
        "Content-Type header is missing or invalid"
      )
    }

    "return forbidden when Content-Type header is not the right format" in {
      withAuthorizedTrader()

      val headers = Seq(
        "X-Client-ID"  -> "clientId",
        "Accept"       -> "application/vnd.hmrc.1.0+json",
        "Content-Type" -> "application/xml"
      )
      val result  = getRecordAndWait(url, headers: _*)

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "INVALID_HEADER_PARAMETERS",
        "Content-Type header is missing or invalid"
      )
    }

    "return internal server error if auth throw" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = getRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId with error: runtime exception"
      )
    }

    "return an BAD_REQUEST (400) if recordId is invalid" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(s"http://localhost:$port/$eoriNumber/records/abcdfg-12gt")

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedJson(
        "INVALID_RECORD_ID_PARAMETER",
        "Invalid record ID supplied for eori number provided"
      )
    }

    "return an error if API return an error with non json message" in {
      withAuthorizedTrader()
      stubRouterRequest(404, "error")

      val result = getRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "timestamp" -> timestamp.truncatedTo(ChronoUnit.SECONDS),
        "code"      -> "INTERNAL_SERVER_ERROR",
        "message"   -> "Response body could not be parsed as JSON, body: error"
      )
    }

    "return an error if json cannot be deserialized to the router message" in {
      withAuthorizedTrader()
      stubRouterRequest(200, "{}")

      val result = getRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "timestamp" -> timestamp.truncatedTo(ChronoUnit.SECONDS),
        "code"      -> "INTERNAL_SERVER_ERROR",
        "message"   -> s"Response body could not be read as type ${typeOf[GetRecordResponse]}"
      )
    }

  }

  private def getRecordAndWait(url: String = url) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "clientId",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .get()
    )

  private def getRecordAndWait(url: String, headers: (String, String)*) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .get()
    )

  private def createExpectedJson(code: String, message: String): Any =
    Json.obj(
      "timestamp" -> "2024-06-08T12:12:12Z",
      "code"      -> code,
      "message"   -> message
    )

  private def stubRouterRequest(status: Int, errorResponse: String) =
    wireMock.stubFor(
      WireMock
        .get(routerUrl)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(errorResponse)
        )
    )
}

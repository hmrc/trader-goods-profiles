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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, GetRecordResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.util.UUID

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
  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId = UUID.randomUUID().toString

  private val url = s"http://localhost:$port/$eoriNumber/records/$recordId"
  private val routerUrl = s"/trader-goods-profiles-router/$eoriNumber/records/$recordId"

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
      stubRouterRequest(Json.toJson(createGetRecordResponse(eoriNumber, recordId, timestamp)))

      val result = getRecordAndWait()

      result.status mustBe OK

    }

    "return a record" in {
      val routerResponse = Json.toJson(createGetRecordResponse(eoriNumber, recordId, timestamp))
      withAuthorizedTrader()

      stubRouterRequest(routerResponse)

      val result = getRecordAndWait()

      result.json mustBe routerResponse

      withClue("should add the right headers") {
        verify(getRequestedFor(urlEqualTo(routerUrl))
          .withHeader("X-Client-Id", equalTo("clientId"))
        );
      }
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
        s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Insufficient Enrolments"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = getRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Invalid affinity group Agent from Auth"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = getRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Invalid enrolment parameter from Auth"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = getRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        s"Supplied OAuth token not authorised to access data for given identifier(s) $eoriNumber"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader()

      val result = getRecordAndWait(s"http://localhost:$port/wrongEoriNumber/records/$recordId")

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "Supplied OAuth token not authorised to access data for given identifier(s) wrongEoriNumber"
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
  }

  private def getRecordAndWait(url: String = url) = {
    await(wsClient.url(url)
      .withHttpHeaders("X-Client-Id" -> "clientId")
      .get())
  }

  private def createExpectedJson(code: String, message: String): Any = {
    Json.obj(
      "timestamp" -> "2024-06-08T12:12:12Z",
      "code" -> code,
      "message" -> message
    )
  }

  private def stubRouterRequest(routerResponse: JsValue) = {
    wireMock.stubFor(
      WireMock.get(routerUrl)
        .willReturn(
          ok()
            .withBody(routerResponse.toString())
        )
    )
  }
}

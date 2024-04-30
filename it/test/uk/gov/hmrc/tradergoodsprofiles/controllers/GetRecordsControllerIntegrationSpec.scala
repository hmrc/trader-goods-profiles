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
import com.github.tomakehurst.wiremock.client.WireMock.ok
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.util.UUID

class GetRecordsControllerIntegrationSpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with AuthTestSupport
    with WireMockServerSpec
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp = Instant.now
  private val recordId = UUID.randomUUID().toString

  private val url = s"http://localhost:$port/$eoriNumber/records/$recordId"

  override lazy val app: Application = {
    wireMock.start()
    WireMock.configureFor(wireHost, wireMock.port())

    GuiceApplicationBuilder()
      .overrides(
        bind[AuthConnector].to(authConnector),
        bind[DateTimeService].to(dateTimeService)
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

      val result = await(wsClient.url(url).get())

      result.status mustBe OK

    }

    "return a record" in {
      withAuthorizedTrader()

      wireMock.stubFor(
        WireMock.get(s"/$eoriNumber/records/$recordId")
          .willReturn(
            ok()
              .withBody(routerResponse.toString())
          )
      )

      val result = await(wsClient.url(s"http://localhost:$port/$eoriNumber/records/$recordId").get())

      result.json mustBe routerResponse
    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = await(wsClient.url(url).get())

      result.status mustBe OK
    }
    "return Unauthorised when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = await(wsClient.url(s"http://localhost:$port/$eoriNumber/records/$recordId").get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Insufficient Enrolments"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = await(wsClient.url(url).get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Invalid affinity group Agent from Auth"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = await(wsClient.url(url).get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> s"Unauthorised exception for /$eoriNumber/records/$recordId with error: Invalid enrolment parameter from Auth"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = await(wsClient.url(url).get())

      result.status mustBe FORBIDDEN
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "FORBIDDEN",
        "message" -> s"Supplied OAuth token not authorised to access data for given identifier(s) $eoriNumber"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader()

      val result = await(wsClient.url(s"http://localhost:$port/wrongEoriNumber/records/$recordId").get())

      result.status mustBe FORBIDDEN
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "FORBIDDEN",
        "message" -> s"Supplied OAuth token not authorised to access data for given identifier(s) wrongEoriNumber"
      )
    }

    "return internal server error if auth throw" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = await(wsClient.url(url).get())

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "INTERNAL_SERVER_ERROR",
        "message" -> s"Internal server error for /$eoriNumber/records/$recordId with error: runtime exception"
      )
    }

    "return an BAD_REQUEST (400) if recordId is invalid" in {
      withAuthorizedTrader()

      val result = await(wsClient.url(s"http://localhost:$port/$eoriNumber/records/abcdfg-12gt").get())

      result.status mustBe BAD_REQUEST
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "INVALID_RECORD_ID_PARAMETER",
        "message" -> s"Invalid record ID supplied for eori $eoriNumber"
      )
    }
  }

  private def routerResponse = {
    Json.obj(
      "eori" -> "GB1234567890",
      "actorId" -> "GB1234567890",
      "recordId" -> "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      "traderRef" -> "BAN001001",
      "comcode" -> "104101000",
      "accreditationRequest" -> "Not requested",
      "goodsDescription" -> "Organic bananas",
      "countryOfOrigin" -> "EC",
      "category" -> 3,
      "assessments" -> Json.arr(
        Json.obj(
          "assessmentId" -> "abc123",
          "primaryCategory" -> "1",
          "condition" -> Json.obj(
            "type" -> "abc123",
            "conditionId" -> "Y923",
            "conditionDescription" -> "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
            "conditionTraderText" -> "Excluded product"
          ))),
      "supplementaryUnit" -> 500,
      "measurementUnit" -> "square meters(m^2)",
      "comcodeEffectiveFromDate" -> "2024-11-18T23:20:19Z",
      "comcodeEffectiveToDate" -> "",
      "version" -> 1,
      "active" -> true,
      "toReview" -> false,
      "reviewReason" -> null,
      "declarable" -> "IMMI declarable",
      "ukimsNumber" -> "XIUKIM47699357400020231115081800",
      "nirmsNumber" -> "RMS-GB-123456",
      "niphlNumber" -> "6 S12345",
      "locked" -> false,
      "srcSystemName" -> "CDAP",
    "createdDateTime" -> "2024-11-18T23:20:19Z",
    "updatedDateTime" -> "2024-11-18T23:20:19Z"
    )
  }
}

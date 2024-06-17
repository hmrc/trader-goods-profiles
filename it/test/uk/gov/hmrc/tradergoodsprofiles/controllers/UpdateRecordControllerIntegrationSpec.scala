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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.WireMockServerSpec

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class UpdateRecordControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockServerSpec
    with CreateOrUpdateRecordResponseSupport
    with UpdateRecordRequestSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val timestamp          = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url              = s"http://localhost:$port/$eoriNumber/records/$recordId"
  private val routerUrl        = s"/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId"
  private val requestBody      = Json.toJson(createUpdateRecordRequest)
  private val expectedResponse = Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))

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
    stubRouterRequest(OK, expectedResponse.toString())
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

  "UpdateRecordController" should {
    "successfully update a record and return 200" in {
      withAuthorizedTrader()

      val result = updateRecordAndWait()

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        verify(
          putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "return 400 Bad request from router when required request field is missing from assessment array" in {
      stubForRouterBadRequest(400, routerError.toString)
      withAuthorizedTrader()

      val result = updateRecordAndWait(invalidUpdateRecordRequestDataForAssessmentArray)

      result.status mustBe BAD_REQUEST
      result.json mustBe routerError
    }

    "return Forbidden when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = updateRecordAndWaitWithoutClientIdHeader()

      result.status mustBe BAD_REQUEST
      result.json mustBe updateExpectedError(
        "INVALID_HEADER_PARAMETER",
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = updateRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe updateExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = updateRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe updateExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe updateExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Internal server error if auth throws" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = updateRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe updateExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId with error: runtime exception"
      )
    }

  }
  val routerError                                        = Json.obj(
    "correlationId" -> correlationId,
    "code"          -> "BAD_REQUEST",
    "message"       -> "Bad Request",
    "errors"        -> Json.arr(
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Optional field assessmentId is in the wrong format",
        "errorNumber" -> 15
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Optional field primaryCategory is in the wrong format",
        "errorNumber" -> 16
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Optional field type is in the wrong format",
        "errorNumber" -> 17
      ),
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Optional field conditionId is in the wrong format",
        "errorNumber" -> 18
      )
    )
  )
  private def updateRecordAndWaitWithoutClientIdHeader() =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .patch(requestBody)
    )

  private def updateRecordAndWait(requestBody: JsValue = requestBody) =
    await(
      wsClient
        .url(url)
        .withHttpHeaders(
          "X-Client-ID"  -> "clientId",
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        .patch(requestBody)
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

  private def updateExpectedError(code: String, message: String, errorNumber: Int): Any =
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

  private def updateExpectedJson(code: String, message: String): Any =
    Json.obj(
      "correlationId" -> correlationId,
      "code"          -> code,
      "message"       -> message
    )

  private def stubForRouterBadRequest(status: Int, responseBody: String) =
    wireMock.stubFor(
      put(routerUrl)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )
  lazy val invalidUpdateRecordRequestDataForAssessmentArray: JsValue     = Json
    .parse("""
             |{
             |    "recordId": "b2fa315b-2d31-4629-90fc-a7b1a5119873",
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "abc123",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        },
             |        {
             |            "assessmentId": "",
             |            "primaryCategory": "test",
             |            "condition": {
             |                "type": "",
             |                "conditionId": "",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)
}

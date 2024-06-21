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

class CreateRecordControllerIntegrationSpec
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
  private val timestamp               = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]
  private val correlationId           = "d677693e-9981-4ee3-8574-654981ebe606"

  private val url              = s"http://localhost:$port/$eoriNumber/records"
  private val routerUrl        = s"/trader-goods-profiles-router/traders/$eoriNumber/records"
  private val requestBody      = createUpdateRecordRequestData
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
    stubRouterRequest(CREATED, expectedResponse.toString())
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

    "successfully create a record without condition and return 201" in {
      withAuthorizedTrader()

      val result = createRecordWithoutConditionAndWait()

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


    "return Unsupported media type when Content-Type header is empty or invalid" in {
      withAuthorizedTrader()

      val headers = Seq("X-Client-ID" -> "clientId", "Content-Type" -> "", "Accept" -> "application/vnd.hmrc.1.0+json")
      val result  = await(
        wsClient
          .url(url)
          .withHttpHeaders(headers: _*)
          .post(requestBody)
      )

      result.status mustBe UNSUPPORTED_MEDIA_TYPE
      result.json mustBe Json.obj(
        "statusCode" -> 415,
        "message"    -> "Expecting text/json or application/json body"
      )
    }

    "return BadRequest when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = createRecordAndWaitWithoutClientIdHeader()

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    "return BadRequest for invalid request body" in {
      stubForRouterBadRequest(BAD_REQUEST, Some(routerError.toString()))
      withAuthorizedTrader()
      val invalidRequestBody = Json.obj()

      val result = createRecordAndWait(invalidRequestBody)

      result.status mustBe BAD_REQUEST
      result.json mustBe routerError
    }

    "return Forbidden when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = createRecordAndWaitWithoutClientIdHeader()

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = createRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = createRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect"
      )
    }

    "return Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = createRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile"
      )
    }

    "return Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = createRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = createRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
      )
    }

    "return Internal server error if auth throws" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = createRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records with error: runtime exception"
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

  val routerError                                                                                      = Json.obj(
    "correlationId" -> correlationId,
    "code"          -> "BAD_REQUEST",
    "message"       -> "Bad Request",
    "errors"        -> Seq(
      createBadRequestJson("Mandatory field actorId was missing from body or is in the wrong format", 8),
      createBadRequestJson("Mandatory field traderRef was missing from body or is in the wrong format", 9),
      createBadRequestJson("Mandatory field comcode was missing from body or is in the wrong format", 11),
      createBadRequestJson("Mandatory field goodsDescription was missing from body or is in the wrong format", 12),
      createBadRequestJson("Mandatory field countryOfOrigin was missing from body or is in the wrong format", 13),
      createBadRequestJson("Mandatory field category was missing from body or is in the wrong format", 14),
      createBadRequestJson(
        "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format",
        23
      )
    )
  )
  private def createRecordWithoutConditionAndWait(requestBody: JsValue = createRecordWithoutCondition) =
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

  private def createBadRequestJson(message: String, errorNumber: Int) =
    Json.obj(
      "code"        -> "INVALID_REQUEST_PARAMETER",
      "message"     -> message,
      "errorNumber" -> errorNumber
    )

  private def stubForRouterBadRequest(status: Int, responseBody: Option[String] = None) =
    wireMock.stubFor(
      post(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody.orNull)
        )
    )
  lazy val invalidCreateRecordRequestDataForAssessmentArray: JsValue                    = Json
    .parse("""
             |{
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

  lazy val createRecordWithoutCondition: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112",
             |    "traderRef": "BAN001001",
             |    "comcode": "10410100",
             |    "goodsDescription": "Organic bananas",
             |    "countryOfOrigin": "EC",
             |    "category": 1,
             |    "assessments": [
             |        {
             |            "assessmentId": "abc123",
             |            "primaryCategory": 1
             |        }
             |    ],
             |    "supplementaryUnit": 500,
             |    "measurementUnit": "Square metre (m2)",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

}

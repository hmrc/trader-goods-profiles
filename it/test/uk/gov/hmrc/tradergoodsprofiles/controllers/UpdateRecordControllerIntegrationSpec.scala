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
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import io.lemonlabs.uri.Url
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
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.libs.ws.{WSClient, writeableOf_JsValue}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.UpdateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.CreateOrUpdateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.support.JsonHelper

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class UpdateRecordControllerIntegrationSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with HttpClientV2Support
    with AuthTestSupport
    with WireMockSupport
    with CreateOrUpdateRecordResponseSupport
    with UpdateRecordRequestSupport
    with BeforeAndAfterEach
    with BeforeAndAfterAll
      with JsonHelper {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val timestamp          = Instant.parse("2024-06-08T12:12:12.456789Z")
  private val recordId                = UUID.randomUUID().toString
  private val uuidService             = mock[UuidService]

  private val url              = s"http://localhost:$port/$eoriNumber/records/$recordId"
  private val routerUrl        = s"/trader-goods-profiles-router/traders/$eoriNumber/records/$recordId"
  private val requestBody      = createUpdateRecordRequestData
  private val expectedResponse = Json.toJson(createCreateOrUpdateRecordResponse(recordId, eoriNumber, timestamp))
  lazy private val appConfig   = mock[AppConfig]

  lazy val configureServices: Map[String, Any] =
    Map(
      "microservice.services.trader-goods-profiles-router.host" -> wireMockHost,
      "microservice.services.trader-goods-profiles-router.port" -> wireMockPort,
      "microservice.services.user-allow-list.host"              -> wireMockHost,
      "microservice.services.user-allow-list.port"              -> wireMockPort,
      "features.userAllowListEnabled"                            -> true
    )
  private val routerError = Json.obj(
    "correlationId" -> correlationId,
    "code"          -> "BAD_REQUEST",
    "message"       -> "Bad Request",
    "errors"        -> Seq(
      Json.obj(
        "code"        -> "INVALID_REQUEST_PARAMETER",
        "message"     -> "Mandatory field actorId was missing from body or is in the wrong format",
        "errorNumber" -> 8
      )
    )
  )

  override lazy val app: Application = {
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
    when(appConfig.sendClientId).thenReturn(true)
    when(appConfig.userAllowListEnabled).thenReturn(true)
    when(appConfig.routerUrl).thenReturn(Url.parse(wireMockUrl))
    when(appConfig.userAllowListBaseUrl).thenReturn(Url.parse(wireMockUrl))

  }



  "patch" should {

    "successfully update a record and return 200" in {
      withAuthorizedTrader()

      val result = updateRecordAndWait()

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        WireMock.verify(
          WireMock.patchRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("X-Client-ID", equalTo("clientId"))
        )
      }
    }

    "should not validate client ID is features flag sendClientId is false" in {
      withAuthorizedTrader()
      when(appConfig.sendClientId).thenReturn(false)
      val result = updateRecordAndWaitWithoutClientIdHeader()

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
        WireMock.verify(
          WireMock.patchRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
        )
      }
    }

    "return BadRequest for invalid request body" in {
      stubRouterRequest(400, routerError.toString)
      withAuthorizedTrader()
      val invalidRequestBody = Json.obj()

      val result = updateRecordAndWait(invalidRequestBody)

      result.status mustBe BAD_REQUEST
      result.json mustBe routerError

    }

    "return Forbidden when X-Client-ID header is missing" in {
      withAuthorizedTrader()

      val result = updateRecordAndWaitWithoutClientIdHeader()

      result.status mustBe BAD_REQUEST
      result.json mustBe createExpectedError(
        "INVALID_HEADER_PARAMETER",
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    "return Forbidden when EORI number is not authorized" in {
      withAuthorizedTrader(enrolment = Enrolment("OTHER-ENROLMENT-KEY"))

      val result = updateRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect",
        Some("103")
      )
    }

    "return Forbidden when identifier does not exist" in {
      withUnauthorizedEmptyIdentifier()

      val result = updateRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "EORI number is incorrect",
        Some("103")
      )
    }

    "return Unauthorized when invalid enrolment" in {
      withUnauthorizedTrader(InsufficientEnrolments())

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"The details signed in do not have a Trader Goods Profile",
        Some("101")
      )
    }

    "return Unauthorized when affinity group is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'",
        Some("102")
      )
    }

    "return Unauthorized when affinity group is empty" in {
      authorizeWithAffinityGroup(None)

      val result = updateRecordAndWait()

      result.status mustBe UNAUTHORIZED
      result.json mustBe createExpectedJson(
        "UNAUTHORIZED",
        "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'",
        Some("102")
      )
    }

    "return Internal server error if auth throws" in {
      withUnauthorizedTrader(new RuntimeException("runtime exception"))

      val result = updateRecordAndWait()

      result.status mustBe INTERNAL_SERVER_ERROR
      result.json mustBe createExpectedJson(
        "INTERNAL_SERVER_ERROR",
        s"Internal server error for /$eoriNumber/records/$recordId with error: runtime exception"
      )
    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubForUserAllowListWhereUserItNotAllowed

      val result = updateRecordAndWait()

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }

  }

  "put" should {
    "successfully update a record and return 200" in {
      withAuthorizedTrader()
      stubRouterPutRequest(OK, expectedResponse.toString())

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Accept"       -> "application/vnd.hmrc.1.0+json",
            "Content-Type" -> "application/json"
          )
          .put(requestBody)
      )

      result.status mustBe OK
      result.json mustBe expectedResponse

      withClue("should add the right headers") {
      WireMock.verify(
        WireMock.putRequestedFor(urlEqualTo(routerUrl))
            .withHeader("Content-Type", equalTo("application/json"))
            .withHeader("Accept", equalTo("application/vnd.hmrc.1.0+json"))
        )
      }
    }

    "return an UNAUTHORIZED error" in {
      withUnauthorizedTrader(InsufficientEnrolments("error"))
      stubRouterPutRequest(OK, expectedResponse.toString())

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Accept"       -> "application/vnd.hmrc.1.0+json",
            "Content-Type" -> "application/json"
          )
          .put(requestBody)
      )

      result.status mustBe UNAUTHORIZED
    }

    "return an error from the router" in {

      val routerErrorResponse = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Seq(
          Json.obj(
            "code"        -> "INVALID_HEADER_PARAMETER",
            "message"     -> "Accept was missing from Header or is in wrong format",
            "errorNumber" -> 11
          )
        )
      )

      stubRouterPutRequest(400, routerErrorResponse.toString)
      withAuthorizedTrader()

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Accept"       -> "application/vnd.hmrc.1.0+json",
            "Content-Type" -> "application/json"
          )
          .put(requestBody)
      )


      result.status mustBe BAD_REQUEST
      result.json mustBe routerErrorResponse

    }

    "return forbidden when EORI is not on the user allow list" in {
      withAuthorizedTrader()
      stubRouterPutRequest(OK, expectedResponse.toString())
      stubForUserAllowListWhereUserItNotAllowed

      val result = await(
        wsClient
          .url(url)
          .withHttpHeaders(
            "Accept"       -> "application/vnd.hmrc.1.0+json",
            "Content-Type" -> "application/json"
          )
          .put(requestBody)
      )

      result.status mustBe FORBIDDEN
      result.json mustBe createExpectedJson(
        "FORBIDDEN",
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      )
    }
  }

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

  private def stubRouterRequest(status: Int, responseBody: String)                      =
    stubFor(
      patch(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

  private def stubRouterPutRequest(status: Int, responseBody: String)                      =
    stubFor(
      put(urlEqualTo(routerUrl))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

  val invalidUpdateRecordRequestDataForAssessmentArray: JsValue = Json
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

  def stubForUserAllowList: StubMapping =
    stubFor(
      post(urlEqualTo(s"/user-allow-list/trader-goods-profiles/private-beta/check"))
        .willReturn(
          aResponse()
            .withStatus(200)
        )
    )

  def stubForUserAllowListWhereUserItNotAllowed: StubMapping =
    stubFor(
      post(urlEqualTo(s"/user-allow-list/trader-goods-profiles/private-beta/check"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )
}

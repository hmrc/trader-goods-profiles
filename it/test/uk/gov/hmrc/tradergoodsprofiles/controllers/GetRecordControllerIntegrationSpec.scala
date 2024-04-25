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

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant

class GetRecordControllerIntegrationSpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with AuthTestSupport
    with BeforeAndAfterEach {

  private lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  private lazy val dateTimeService = mock[DateTimeService]
  private lazy val timestamp = Instant.now
  private val url = s"http://localhost:$port/$eoriNumber/records"


  override lazy val app: Application = {

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
  "GET record" should {
    "return 200" in {
      withAuthorizedTrader

      val result = await(wsClient.url(url).get())

      result.status mustBe OK

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

      val result = await(wsClient.url(url).get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> "Unauthorised exception for /GB000000000123/records with error: Insufficient Enrolments"
      )
    }

    "return Unauthorised when affinityGroup is Agent" in {
      authorizeWithAffinityGroup(Some(Agent))

      val result = await(wsClient.url(url).get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> "Unauthorised exception for /GB000000000123/records with error: Invalid affinity group Agent from Auth"
      )
    }

    "return Unauthorised when affinityGroup empty" in {
      authorizeWithAffinityGroup(None)

      val result = await(wsClient.url(url).get())

      result.status mustBe UNAUTHORIZED
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "UNAUTHORIZED",
        "message" -> "Unauthorised exception for /GB000000000123/records with error: Invalid enrolment parameter from Auth"
      )
    }

    "return forbidden if identifier does not exist" in {
      withUnauthorizedEmptyIdentifier

      val result = await(wsClient.url(url).get())

      result.status mustBe FORBIDDEN
      result.json mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "FORBIDDEN",
        "message" -> s"Supplied OAuth token not authorised to access data for given identifier(s) $eoriNumber"
      )
    }

    "return forbidden if identifier is not authorised" in {
      withAuthorizedTrader

      val result = await(wsClient.url(s"http://localhost:$port/wrongEoriNumber/records").get())

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
        "message" -> "Internal server error for /GB000000000123/records with error: runtime exception"
      )
    }
  }
}

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

package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsJson, defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.auth.EnrolmentRequest
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
  extends PlaySpec
    with AuthTestSupport
    with BeforeAndAfterEach {

  implicit val ec = ExecutionContext.Implicits.global

  private val timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
  private val dateTimeService = mock[DateTimeService]
  private val parser = mock[BodyParsers.Default]
  private val appConfig = mock[AppConfig]

  private val sut = new AuthActionImpl(
    authConnector,
    dateTimeService,
    appConfig,
    parser,
    stubMessagesControllerComponents(),
    mock[BodyParsers.Default]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector, dateTimeService, appConfig)

    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(appConfig.tgpIdentifier).thenReturn(tgpIdentifierName)
  }

  "authorisation" should {
    "authorise an enrolment with affinitygroup as organisation" in {
      withAuthorizedTrader

      val result = await(sut.apply(eroiNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eroiNumber)

      withAuthorizedTrader(enrolment)

      val result = await(sut.apply(eroiNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "authorise an enrolment with affinitygroup as Individual" in {
      authorizeWithAffinityGroup(Some(Individual))

      val result = await(sut.apply(eroiNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "return unauthorized" when {
      "invalid enrolment" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = sut.apply(eroiNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code" -> "UNAUTHORIZED",
          "message" -> "Unauthorised exception for /get with error: Insufficient Enrolments"
        )
      }

      "affinity group is Agent" in {
        authorizeWithAffinityGroup(Some(Agent))

        val result = sut.apply(eroiNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code" -> "UNAUTHORIZED",
          "message" -> "Unauthorised exception for /get with error: Invalid affinity group Agent from Auth"
        )
      }

      "cannot find affinityGroup" in {
        authorizeWithAffinityGroup(None)

        val result = sut.apply(eroiNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "timestamp" -> timestamp,
          "code" -> "UNAUTHORIZED",
          "message" -> "Unauthorised exception for /get with error: Invalid enrolment parameter from Auth"
        )
      }
    }

    "return internal server error when throwing" in {

      withUnauthorizedTrader(new RuntimeException("unauthorised error"))

      val result = sut.apply(eroiNumber).invokeBlock(FakeRequest("GET", "/get"), block)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "INTERNAL_SERVER_ERROR",
        "message" -> "Internal server error for /get with error: unauthorised error"
      )
    }

    "return forbidden if identifier is missing" in {
      withUnauthorizedEmptyIdentifier

      val result: Future[Result] = sut.apply(eroiNumber).invokeBlock(FakeRequest(), block)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "FORBIDDEN",
        "message" -> s"Supplied OAuth token not authorised to access data for given identifier(s) $eroiNumber"
      )
    }

    "return forbidden if identifier is unauthorized" in {
      withAuthorizedTrader

      val result = sut.apply("any-roi").invokeBlock(FakeRequest(), block)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj(
        "timestamp" -> timestamp,
        "code" -> "FORBIDDEN",
        "message" -> s"Supplied OAuth token not authorised to access data for given identifier(s) any-roi"
      )
    }
  }

  def block(request: EnrolmentRequest[_]):  Future[Result] =
    Future.successful(Results.Ok)
}

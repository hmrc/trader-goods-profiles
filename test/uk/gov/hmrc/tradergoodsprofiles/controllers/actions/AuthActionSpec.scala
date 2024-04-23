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
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Results.{InternalServerError, Unauthorized}
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.{Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.ErrorResponse
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

  private val sut = new AuthActionImpl(
    authConnector,
    dateTimeService,
    stubMessagesControllerComponents(),
    mock[BodyParsers.Default]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector)

    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  "authorisation" should {
    "authorise an enrolment with affinitygroup as organisation" in {
      withAuthorizedTrader

      val result = await(sut.invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "authorise an enrolment with affinitygroup as Individual" in {
      authorizeWithAffinityGroup(Some(Individual))

      val result = await(sut.invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "return unauthorized" when {
      "invalid enrolment" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

        result mustBe Unauthorized(Json.toJson(ErrorResponse(
          timestamp,
          "Unauthorised",
          "Unauthorised error for /get with error: Insufficient Enrolments"
        )))
      }

      "affinity group is Agent" in {
        authorizeWithAffinityGroup(Some(Agent))

        val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

        result mustBe Unauthorized(Json.toJson(ErrorResponse(
          timestamp,
          "Unauthorised",
          "Unauthorised error for /get with error: Invalid affinity group Agent from Auth"
        )))
      }

      "cannot find affinityGroup" in {
        authorizeWithAffinityGroup(None)

        val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

        result mustBe Unauthorized(Json.toJson(ErrorResponse(
          timestamp,
          "Unauthorised",
          "Unauthorised error for /get with error: Invalid enrolment parameter from Auth"
        )))
      }
    }

    "return internal server error when throwing" in {

      withUnauthorizedTrader(new RuntimeException("unauthorised error"))

      val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

      result mustBe InternalServerError(Json.toJson(ErrorResponse(
        timestamp,
        "Internal server error",
        "Internal server error for /get with error unauthorised error"
      )))
    }
  }

  def block(request: EnrolmentRequest[_]):  Future[Result] = Future.successful(Results.Ok)
}

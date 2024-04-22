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

import org.mockito.ArgumentMatchers
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
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, Enrolments, InsufficientEnrolments}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.tradergoodsprofiles.models.ErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.models.auth.EnrolmentRequest
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
  extends PlaySpec
  with BeforeAndAfterEach {
  implicit val ec = ExecutionContext.Implicits.global

  private val timestamp = Instant.now.truncatedTo(ChronoUnit.SECONDS)
  private val authConnector: AuthConnector = mock[AuthConnector]
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

    val retrieval = Enrolments(Set(Enrolment("HMRC-EMCS-ORG")))  and Some(Organisation)
    val authFetch = authorisedEnrolments and affinityGroup
    when(authConnector.authorise(ArgumentMatchers.argThat((p: Predicate) => true), eqTo(authFetch))(any,any))
      .thenReturn(Future.successful(retrieval))
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  "authorisation" should {
    "authorise an enrolment" in {
      val result = await(sut.invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "return unauthorized" when {
      "invalid enrolment" in {
        when(authConnector.authorise(any, any)(any, any))
          .thenReturn(Future.failed(InsufficientEnrolments()))

        val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

        result mustBe Unauthorized(Json.toJson(ErrorResponse(
          timestamp,
          "Unauthorised",
          "Unauthorised Exception for /get with error Insufficient Enrolments"
        )))
      }
    }

    "return internal server error when throwing" in {
        when(authConnector.authorise(any, any)(any, any))
          .thenReturn(Future.failed(new RuntimeException("unauthorised error")))

        val result = await(sut.invokeBlock(FakeRequest("GET", "/get"), block))

        result mustBe InternalServerError(Json.toJson(ErrorResponse(
          timestamp,
          "Internal server error",
          "Unauthorised Exception for /get with error unauthorised error"
        )))
      }
    }

  def block(request: EnrolmentRequest[_]):  Future[Result] = Future.successful(Results.Ok)
}

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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.auth.EnrolmentRequest
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val uuidService   = mock[UuidService]
  private val parser        = mock[BodyParsers.Default]
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"

  private val sut = new AuthActionImpl(
    authConnector,
    uuidService,
    parser,
    stubMessagesControllerComponents(),
    mock[BodyParsers.Default]
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(authConnector, uuidService)

    when(uuidService.uuid).thenReturn(correlationId)
  }

  "authorisation" should {
    "authorise an enrolment with affinitygroup as organisation" in {
      withAuthorizedTrader()

      val result = await(sut.apply(eoriNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "authorise an enrolment with multiple identifier" in {
      val enrolment = Enrolment(enrolmentKey)
        .withIdentifier(tgpIdentifierName, "GB000000000122")
        .withIdentifier(tgpIdentifierName, eoriNumber)

      withAuthorizedTrader(enrolment)

      val result = await(sut.apply(eoriNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "authorise an enrolment with affinitygroup as Individual" in {
      authorizeWithAffinityGroup(Some(Individual))

      val result = await(sut.apply(eoriNumber).invokeBlock(FakeRequest(), block))

      result.header.status mustBe OK
      verify(authConnector).authorise(eqTo(Enrolment("HMRC-CUS-ORG")), any)(any, any)
    }

    "return unauthorized" when {
      "invalid enrolment" in {
        withUnauthorizedTrader(InsufficientEnrolments())

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "The details signed in do not have a Trader Goods Profile"
        )
      }

      "affinity group is Agent" in {
        authorizeWithAffinityGroup(Some(Agent))

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
        )
      }

      "cannot find affinityGroup" in {
        authorizeWithAffinityGroup(None)

        val result = sut.apply(eoriNumber).invokeBlock(FakeRequest("GET", "/get"), block)

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe Json.obj(
          "correlationId" -> correlationId,
          "code"          -> "UNAUTHORIZED",
          "message"       -> "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
        )
      }
    }

    "return internal server error when throwing" in {

      withUnauthorizedTrader(new RuntimeException("unauthorised error"))

      val result = sut.apply(eoriNumber).invokeBlock(FakeRequest("GET", "/get"), block)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Internal server error for /get with error: unauthorised error"
      )
    }

    "return forbidden if identifier is missing" in {
      withUnauthorizedEmptyIdentifier()

      val result: Future[Result] = sut.apply(eoriNumber).invokeBlock(FakeRequest(), block)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> s"EORI number is incorrect"
      )
    }

    "return forbidden if identifier is unauthorized" in {
      withAuthorizedTrader()

      val result = sut.apply("any-roi").invokeBlock(FakeRequest(), block)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "FORBIDDEN",
        "message"       -> s"EORI number is incorrect"
      )
    }
  }

  def block(request: EnrolmentRequest[_]): Future[Result] =
    Future.successful(Results.Ok)
}

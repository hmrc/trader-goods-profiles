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

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tradergoodsprofiles.connectors.UserAllowListConnector
import uk.gov.hmrc.tradergoodsprofiles.connectors.UserAllowListConnector.UnexpectedResponseException
import uk.gov.hmrc.tradergoodsprofiles.models.UserRequest
import uk.gov.hmrc.tradergoodsprofiles.models.errors.UserNotAllowedResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import scala.concurrent.{ExecutionContext, Future}

class UserAllowListActionSpec extends AnyWordSpec with Matchers with ScalaFutures with EitherValues {

  private val connector             = mock[UserAllowListConnector]
  private val uuidService           = mock[UuidService]
  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  private val sut = new UserAllowListActionImpl(connector, uuidService)

  "check action" should {
    "return a unit if the user is allowed" in {
      val request = UserRequest(FakeRequest(), "12345")
      when(connector.check(any, any)(any)).thenReturn(Future.successful(true))

      val result = await(sut.refine(request))

      result shouldBe Right(request)
    }

    "return a UserNotAllowedResponse when the user is not allowed access" in {
      val correlationId          = "246f01c0-a9b3-498e-a7c9-dcbe2f9a4151"
      val userNotAllowedResponse = UserNotAllowedResponse(
        correlationId,
        "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
      ).toResult
      val request                = UserRequest(FakeRequest(), "12345")

      when(connector.check(any, any)(any)).thenReturn(Future.successful(false))
      when(uuidService.uuid).thenReturn(correlationId)

      val result = await(sut.refine(request))

      result.left.value shouldBe userNotAllowedResponse
    }

    "should return an error thrown by the userAllowListConnect" in {
      val request                                = UserRequest(FakeRequest(), "12345")
      val exception: UnexpectedResponseException = UnexpectedResponseException(400)

      when(connector.check(any, any)(any)).thenReturn(Future.failed(exception))

      assertThrows[UnexpectedResponseException](await(sut.refine(request)))
    }

  }
}

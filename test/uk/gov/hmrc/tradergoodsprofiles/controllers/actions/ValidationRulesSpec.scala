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

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.util.UUID

class ValidationRulesSpec extends PlaySpec with EitherValues with BeforeAndAfterEach {

  private val correlationId = UUID.randomUUID().toString
  private val uuidService   = mock[UuidService]

  class TestValidationRules(override val uuidService: UuidService) extends BackendBaseController with ValidationRules {

    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "validateDefaultHeaders" should {
    "validate all the headers" in new TestValidationRules(uuidService) { validator =>
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(
        "Accept"       -> "application/vnd.hmrc.1.0+json",
        "Content-Type" -> "application/json",
        "X-Client-ID"  -> "some client ID"
      )

      val result = validator.validateAllHeaders(request)

      result mustBe Right(())
    }

    "return invalid Content-Type error" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Accept"      -> "application/vnd.hmrc.1.0+json",
        "X-Client-ID" -> "some client ID"
      )

      val result = validator.validateAllHeaders(request)

      result.left.value mustBe createExpectedError(
        "Content-Type was missing from Header or is in the wrong format",
        3
      )

    }

    "return Accept header error" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Content-Type" -> "application/json",
        "X-Client-ID"  -> "some client ID"
      )

      val result = validator.validateAllHeaders(request)

      result.left.value mustBe createExpectedError(
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return Client ID header error" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Accept"       -> "application/vnd.hmrc.1.0+json",
        "Content-Type" -> "application/json"
      )

      val result = validator.validateAllHeaders(request)

      result.left.value mustBe createExpectedError(
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }

    "return the first invalid header error when all header are invalid" in new TestValidationRules(uuidService) {
      validator =>
      val result = validator.validateAllHeaders(FakeRequest())

      result.left.value mustBe createExpectedError(
        "Accept was missing from Header or is in wrong format",
        4
      )
    }
  }

  "validateAcceptAndClientIdHeader" should {
    "validate headers" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Accept"      -> "application/vnd.hmrc.1.0+json",
        "X-Client-ID" -> "some client ID"
      )

      val result = validator.validateAcceptAndClientIdHeaders(request)

      result mustBe Right(())
    }

    "return Accept header error" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Content-Type" -> "application/json",
        "X-Client-ID"  -> "some client ID"
      )

      val result = validator.validateAcceptAndClientIdHeaders(request)

      result.left.value mustBe createExpectedError(
        "Accept was missing from Header or is in wrong format",
        4
      )
    }

    "return Client ID header error" in new TestValidationRules(uuidService) {
      validator =>
      val request = FakeRequest().withHeaders(
        "Accept"       -> "application/vnd.hmrc.1.0+json",
        "Content-Type" -> "application/json"
      )

      val result = validator.validateAcceptAndClientIdHeaders(request)

      result.left.value mustBe createExpectedError(
        "X-Client-ID was missing from Header or is in wrong format",
        6000
      )
    }
  }

  private def createExpectedError(message: String, errorNumber: Int): Result =
    BadRequest(
      Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "BAD_REQUEST",
        "message"       -> "Bad Request",
        "errors"        -> Json.arr(
          Json.obj(
            "code"        -> "INVALID_HEADER_PARAMETER",
            "message"     -> message,
            "errorNumber" -> errorNumber
          )
        )
      )
    )

}

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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.BadRequest
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import scala.concurrent.ExecutionContext

class ValidateHeaderActionSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val uuidService   = mock[UuidService]
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val sut           = new ValidateHeaderAction(uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService)
    when(uuidService.uuid).thenReturn(correlationId)
  }

  "Validate Header Action" should {
    "return None" when {
      "accept header is valid" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json",
          "X-Client-ID"  -> "some client ID"
        )
        val result  = await(sut.filter(request))

        result mustBe None
      }
    }

    "return a bad request" when {
      "accept header is missing" in {
        val request = FakeRequest().withHeaders("Content-Type" -> "application/json", "X-Client-ID" -> "some client ID")
        val result  = await(sut.filter(request))
        result.value mustBe BadRequest(
          createExpectedJson("INVALID_HEADER_PARAMETER", "Accept was missing from Header or is in wrong format", 4)
        )
      }

      "content type header is missing" in {
        val request =
          FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "X-Client-ID" -> "some client ID")
        val result  = await(sut.filter(request))
        result.value mustBe BadRequest(
          createExpectedJson(
            "INVALID_HEADER_PARAMETER",
            "Content-Type was missing from Header or is in the wrong format",
            3
          )
        )
      }

      "client ID header is missing" in {
        val request =
          FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Content-Type" -> "application/json")
        val result  = await(sut.filter(request))
        result.value mustBe BadRequest(
          createExpectedJson(
            "INVALID_HEADER_PARAMETER",
            "X-Client-ID was missing from Header or is in wrong format",
            6000
          )
        )
      }

      "accept header is the incorrect format" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "the wrong format",
          "Content-Type" -> "application/json",
          "X-Client-ID"  -> "some client ID"
        )
        val result  = await(sut.filter(request))

        result.value mustBe BadRequest(
          createExpectedJson("INVALID_HEADER_PARAMETER", "Accept was missing from Header or is in wrong format", 4)
        )
      }

      "content type header is the incorrect format" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "the wrong format",
          "X-Client-ID"  -> "some client ID"
        )
        val result  = await(sut.filter(request))

        result.value mustBe BadRequest(
          createExpectedJson(
            "INVALID_HEADER_PARAMETER",
            "Content-Type was missing from Header or is in the wrong format",
            3
          )
        )
      }
    }
  }

  private def createExpectedJson(code: String, message: String, errorNumber: Int): JsObject =
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
}

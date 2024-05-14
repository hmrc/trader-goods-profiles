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
import play.api.mvc.Results.Forbidden
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant
import scala.concurrent.ExecutionContext

class ValidateHeaderActionSpec extends PlaySpec with BeforeAndAfterEach with EitherValues {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val dateTimeService = mock[DateTimeService]
  private val sut             = new ValidateHeaderAction(dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(dateTimeService)
    when(dateTimeService.timestamp).thenReturn(Instant.parse("2024-05-09T12:12:12.5678985Z"))
  }

  "Validate Header Action" should {
    "return None" when {
      "accept header is valid" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "application/json"
        )
        val result  = await(sut.filter(request))

        result mustBe None
      }
    }

    "return a forbidden" when {
      "accept header is missing" in {
        val request = FakeRequest().withHeaders("Content-Type" -> "application/json")
        val result  = await(sut.filter(request))
        result.value mustBe Forbidden(createExpectedJson("The accept header is missing"))
      }

      "content type header is missing" in {
        val request = FakeRequest().withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")
        val result  = await(sut.filter(request))
        result.value mustBe Forbidden(createExpectedJson("The Content-Type header is missing"))
      }

      "accept header is the incorrect format" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "the wrong format",
          "Content-Type" -> "application/json"
        )
        val result  = await(sut.filter(request))

        result.value mustBe Forbidden(createExpectedJson("Invalid Header"))
      }

      "content type header is the incorrect format" in {
        val request = FakeRequest().withHeaders(
          "Accept"       -> "application/vnd.hmrc.1.0+json",
          "Content-Type" -> "the wrong format"
        )
        val result  = await(sut.filter(request))

        result.value mustBe Forbidden(createExpectedJson("Invalid Header"))
      }
    }
  }

  private def createExpectedJson(message: String): JsObject =
    Json.obj(
      "timestamp" -> "2024-05-09T12:12:12Z",
      "code"      -> "INVALID_HEADER_PARAMETERS",
      "message"   -> message
    )
}

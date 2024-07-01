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

package uk.gov.hmrc.tradergoodsprofiles.connectors

import org.mockito.MockitoSugar.when
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.util.UUID
import scala.reflect.runtime.universe.typeOf

class RouterHttpReaderSpec extends PlaySpec with EitherValues {

  private val uuidService   = mock[UuidService]
  private val correlationId = UUID.randomUUID().toString

  class TestRouterHttpReader(override val uuidService: UuidService) extends RouterHttpReader

  case class TestResponse(eori: String)
  object TestResponse {
    implicit val format: OFormat[TestResponse] = Json.format[TestResponse]
  }

  "httpReader" should {
    when(uuidService.uuid).thenReturn(correlationId)

    "return the object requested" in new TestRouterHttpReader(uuidService) { reader =>
      val response = HttpResponse(200, Json.toJson(TestResponse("123")), Map.empty)
      val result   = reader.httpReader[TestResponse].read("GET", "any-url", response)

      result.value mustBe TestResponse("123")
    }

    "return an error" when {
      "HttpResponse is an error" in new TestRouterHttpReader(uuidService) { reader =>
        val response     = ErrorResponse("123", "any-code", "error", Some(Seq(Error("BAD_REQUEST", "Bad request", 78890))))
        val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(response), Map.empty)
        val result       = reader.httpReader[TestResponse].read("GET", "any-url", httpResponse)

        result.left.value mustBe ServiceError(BAD_REQUEST, response)
      }
    }

    "cannot parse a success response" in new TestRouterHttpReader(uuidService) { reader =>
      val response = HttpResponse(200, Json.obj(), Map.empty)
      val result   = reader.httpReader[TestResponse].read("GET", "any-url", response)

      result.left.value mustBe ServiceError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          "INTERNAL_SERVER_ERROR",
          s"Response body could not be read as type ${typeOf[TestResponse]}"
        )
      )
    }

    "cannot parse an error response" in new TestRouterHttpReader(uuidService) { reader =>
      val response = HttpResponse(NOT_FOUND, Json.obj(), Map.empty)
      val result   = reader.httpReader[TestResponse].read("GET", "any-url", response)

      result.left.value mustBe ServiceError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          "INTERNAL_SERVER_ERROR",
          s"Response body could not be read as type ${typeOf[ErrorResponse]}"
        )
      )
    }
  }

  "httpReaderWithoutPayload" should {
    when(uuidService.uuid).thenReturn(correlationId)

    "return the object requested" in new TestRouterHttpReader(uuidService) { reader =>
      val response = HttpResponse(200, Json.obj(), Map.empty)
      val result   = reader.httpReaderWithoutResponseBody.read("GET", "any-url", response)

      result.value mustBe 200
    }

    "return an error" when {
      "HttpResponse is an error" in new TestRouterHttpReader(uuidService) { reader =>
        val response     = ErrorResponse("123", "any-code", "error", Some(Seq(Error("BAD_REQUEST", "Bad request", 78890))))
        val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(response), Map.empty)
        val result       = reader.httpReaderWithoutResponseBody.read("GET", "any-url", httpResponse)

        result.left.value mustBe ServiceError(BAD_REQUEST, response)
      }
    }

    "cannot parse an error response" in new TestRouterHttpReader(uuidService) { reader =>
      val response = HttpResponse(NOT_FOUND, Json.obj(), Map.empty)
      val result   = reader.httpReaderWithoutResponseBody.read("GET", "any-url", response)

      result.left.value mustBe ServiceError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse(
          correlationId,
          "INTERNAL_SERVER_ERROR",
          s"Response body could not be read as type ${typeOf[ErrorResponse]}"
        )
      )
    }
  }
}

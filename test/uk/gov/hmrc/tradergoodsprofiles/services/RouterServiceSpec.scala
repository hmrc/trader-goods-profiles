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

package uk.gov.hmrc.tradergoodsprofiles.services

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.GetRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.models.errors.RouterError

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class RouterServiceSpec
    extends PlaySpec
    with GetRecordResponseSupport
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val connector       = mock[RouterConnector]
  private val recordResponse  = createGetRecordResponse("GB123456789012", "recordId", Instant.now)
  private val dateTimeService = mock[DateTimeService]
  private val timestamp       = Instant.parse("2024-12-05T12:12:45Z")

  private val sut = new RouterServiceImpl(connector, dateTimeService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, dateTimeService)

    when(connector.get(any, any)(any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(recordResponse), Map.empty)))
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }
  "getRecord" should {
    "request a record" in {
      val result = sut.getRecord("GB123456789012", "recordId")

      whenReady(result.value) { _ =>
        verify(connector).get(eqTo("GB123456789012"), eqTo("recordId"))(any)
      }
    }

    "return GetRecordResponse" in {
      val result = sut.getRecord("eori", "recordId")

      whenReady(result.value)(_.value mustBe recordResponse)
    }

    "return an error" when {
      "cannot parse the response" in {

        when(connector.get(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(200, Json.obj(), Map.empty)))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "timestamp" -> timestamp,
              "code"      -> "INTERNAL_SERVER_ERROR",
              "message"   -> s"Response body could not be read as type ${typeOf[GetRecordResponse]}"
            )
          )
        }
      }

      "cannot parse the response as Json" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(200, "error")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result.value) {
          _.left.value mustBe createInternalServerErrorResult(
            s"Response body could not be parsed as JSON, body: error"
          )
        }
      }

      "add timestamp to router error" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.successful(createHttpResponse(500, "INTERNAL_SERVER_ERROR")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> "correlationId",
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> "any message",
              "timestamp"     -> timestamp
            )
          )
        }
      }

      "routerConnector return an exception" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "timestamp" -> timestamp,
              "code"      -> "INTERNAL_SERVER_ERROR",
              "message"   -> s"Could not retrieve record for eori number eori and record ID recordId"
            )
          )
        }

      }

      val table = Table(
        ("description", "status", "expectedResult", "code"),
        ("return bad request", 400, 400, "BAD_REQUEST"),
        ("return Forbidden", 403, 403, "FORBIDDEN"),
        ("return Not Found", 404, 404, "NOT_FOUND")
      )

      forAll(table) {
        (
          description: String,
          status: Int,
          expectedResult: Int,
          code: String
        ) =>
          s"$description" in {
            when(connector.get(any, any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.getRecord("eori", "recordId")

            whenReady(result.value) {
              _.left.value.header.status mustBe expectedResult
            }
          }
      }
    }
  }

  "removeRecord" should {
    "return 200 OK " in {
      val httpResponse = HttpResponse(Status.OK, "")
      when(connector.put("GB123456789012", "recordId", "GB123456789012")).thenReturn(Future.successful(httpResponse))

      val result = sut.removeRecord("GB123456789012", "recordId", "GB123456789012").value

      result.map { res =>
        res mustBe Right(())
      }
    }

    "return an error" when {
      "add timestamp to router error" in {
        when(connector.put(any, any, any)(any))
          .thenReturn(Future.successful(createHttpResponse(500, "INTERNAL_SERVER_ERROR")))

        val result = sut.removeRecord("eori", "recordId", "actorId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> "correlationId",
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> "any message",
              "timestamp"     -> timestamp
            )
          )
        }
      }

      "routerConnector return an exception" in {
        when(connector.put(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.removeRecord("eori", "recordId", "actorId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "timestamp" -> timestamp,
              "code"      -> "INTERNAL_SERVER_ERROR",
              "message"   -> s"Could not remove record for eori number eori and record ID recordId"
            )
          )
        }

      }

      val table = Table(
        ("description", "status", "expectedResult", "code"),
        ("return bad request", 400, 400, "BAD_REQUEST"),
        ("return Forbidden", 403, 403, "FORBIDDEN"),
        ("return Not Found", 404, 404, "NOT_FOUND")
      )

      forAll(table) {
        (
          description: String,
          status: Int,
          expectedResult: Int,
          code: String
        ) =>
          s"$description" in {
            when(connector.put(any, any, any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.removeRecord("eori", "recordId", "actorId")

            whenReady(result.value) {
              _.left.value.header.status mustBe expectedResult
            }
          }
      }
    }
  }

  private def createInternalServerErrorResult(message: String): Result =
    InternalServerError(
      Json.obj(
        "timestamp" -> timestamp,
        "code"      -> "INTERNAL_SERVER_ERROR",
        "message"   -> message
      )
    )

  private def createHttpResponse(status: Int, code: String) =
    HttpResponse(status, Json.toJson(RouterError("correlationId", code, "any message")), Map.empty)
}

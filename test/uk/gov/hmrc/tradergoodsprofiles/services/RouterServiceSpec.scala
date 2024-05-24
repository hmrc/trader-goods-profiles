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
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{APICreateRecordRequestSupport, RouterCreateRecordRequestSupport}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.{CreateRecordResponseSupport, GetRecordResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.RouterError
import uk.gov.hmrc.tradergoodsprofiles.models.requests.RouterCreateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.models.response.GetRecordResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class RouterServiceSpec
    extends PlaySpec
    with GetRecordResponseSupport
    with CreateRecordResponseSupport
    with APICreateRecordRequestSupport
    with RouterCreateRecordRequestSupport
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val connector      = mock[RouterConnector]
  private val recordResponse = createGetRecordResponse("GB123456789012", "recordId", Instant.now)
  private val createResponse = createCreateRecordResponse("recordId", "GB123456789012", Instant.now)
  private val uuidService    = mock[UuidService]
  private val correlationId  = "d677693e-9981-4ee3-8574-654981ebe606"

  private val sut = new RouterServiceImpl(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)

    when(connector.get(any, any)(any))
      .thenReturn(Future.successful(HttpResponse(200, Json.toJson(recordResponse), Map.empty)))
    when(uuidService.uuid).thenReturn(correlationId)
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
              "correlationId" -> correlationId,
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> s"Response body could not be read as type ${typeOf[GetRecordResponse]}"
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

      "routerConnector return an exception" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> s"Could not retrieve record for eori number eori and record ID recordId"
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

  "createRecord" should {
    "create a record" in {
      val createRequest = createAPICreateRecordRequest()

      when(connector.post(any)(any))
        .thenReturn(Future.successful(HttpResponse(201, Json.toJson(createResponse), Map.empty)))

      val result = sut.createRecord("GB123456789012", createRequest)

      whenReady(result.value) { _ =>
        verify(connector).post(eqTo(RouterCreateRecordRequest("GB123456789012", createRequest)))(any)
      }
    }

    "return CreateRecordResponse" in {
      val createRequest = createAPICreateRecordRequest()

      when(connector.post(any)(any))
        .thenReturn(Future.successful(HttpResponse(201, Json.toJson(createResponse), Map.empty)))

      val result = sut.createRecord("GB123456789012", createRequest)

      whenReady(result.value)(_.value mustBe createResponse)
    }

    "return an error" when {
      "cannot parse the response" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.post(any)(any))
          .thenReturn(Future.successful(HttpResponse(201, Json.obj(), Map.empty)))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> s"Could not create record due to an internal error"
            )
          )
        }
      }

      "cannot parse the response as Json" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.post(any)(any))
          .thenReturn(Future.successful(HttpResponse(201, "error")))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result.value) {
          _.left.value mustBe createInternalServerErrorResult(
            s"Response body could not be parsed as JSON, body: error"
          )
        }
      }

      "routerConnector return an exception" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.post(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> s"Could not create record due to an internal error"
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
            val createRequest = createAPICreateRecordRequest()

            when(connector.post(any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.createRecord("GB123456789012", createRequest)

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

      "routerConnector return an exception" in {
        when(connector.put(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.removeRecord("eori", "recordId", "actorId")

        whenReady(result.value) {
          _.left.value mustBe InternalServerError(
            Json.obj(
              "correlationId" -> correlationId,
              "code"          -> "INTERNAL_SERVER_ERROR",
              "message"       -> s"Could not remove record for eori number eori and record ID recordId"
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
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> message
      )
    )

  private def createHttpResponse(status: Int, code: String) =
    HttpResponse(status, Json.toJson(RouterError("correlationId", code, "any message")), Map.empty)
}

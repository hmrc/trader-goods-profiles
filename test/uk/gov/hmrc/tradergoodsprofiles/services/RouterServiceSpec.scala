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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{APICreateRecordRequestSupport, RouterCreateRecordRequestSupport, UpdateRecordRequestSupport}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.{CreateOrUpdateRecordResponseSupport, GetRecordResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.router.{RouterRequestAccreditationRequest, RouterUpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.{MaintainProfileRequest, RequestAccreditationRequest, router}
import uk.gov.hmrc.tradergoodsprofiles.models.response.GetRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.models.responses.UpdateProfileResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe.typeOf

class RouterServiceSpec
    extends PlaySpec
    with GetRecordResponseSupport
    with CreateOrUpdateRecordResponseSupport
    with APICreateRecordRequestSupport
    with RouterCreateRecordRequestSupport
    with UpdateRecordRequestSupport
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val connector      = mock[RouterConnector]
  private val recordResponse = createGetRecordResponse("GB123456789012", "recordId", Instant.now)
  private val createResponse = createCreateOrUpdateRecordResponse("recordId", "GB123456789012", Instant.now)
  private val uuidService    = mock[UuidService]
  private val correlationId  = "d677693e-9981-4ee3-8574-654981ebe606"

  private val sut = new RouterService(connector, uuidService)

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

      whenReady(result) { _ =>
        verify(connector).get(eqTo("GB123456789012"), eqTo("recordId"))(any)
      }
    }

    "return GetRecordResponse" in {
      val result = sut.getRecord("eori", "recordId")

      whenReady(result)(_.value mustBe recordResponse)
    }

    "return an error" when {
      "cannot parse the response" in {

        when(connector.get(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(200, Json.obj(), Map.empty)))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              s"Response body could not be read as type ${typeOf[GetRecordResponse]}",
              None
            )
          )
        }
      }

      "cannot parse the response as Json" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.successful(HttpResponse(200, "error")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              s"Response body could not be parsed as JSON, body: error",
              None
            )
          )
        }
      }

      "routerConnector return an exception" in {
        when(connector.get(any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.getRecord("eori", "recordId")

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              s"Could not retrieve record for eori number eori and record ID recordId",
              None
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

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  "createRecord" should {
    "create a record" in {
      val createRequest = createAPICreateRecordRequest()

      when(connector.createRecord(any)(any))
        .thenReturn(Future.successful(HttpResponse(201, Json.toJson(createResponse), Map.empty)))

      val result = sut.createRecord("GB123456789012", createRequest)

      whenReady(result) { _ =>
        verify(connector).createRecord(eqTo(router.RouterCreateRecordRequest("GB123456789012", createRequest)))(any)
      }
    }

    "return CreateRecordResponse" in {
      val createRequest = createAPICreateRecordRequest()

      when(connector.createRecord(any)(any))
        .thenReturn(Future.successful(HttpResponse(201, Json.toJson(createResponse), Map.empty)))

      val result = sut.createRecord("GB123456789012", createRequest)

      whenReady(result)(_.value mustBe createResponse)
    }

    "return an error" when {
      "cannot parse the response" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.createRecord(any)(any))
          .thenReturn(Future.successful(HttpResponse(201, Json.obj(), Map.empty)))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Could not create record due to an internal error",
              None
            )
          )
        }
      }

      "cannot parse the response as Json" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.createRecord(any)(any))
          .thenReturn(Future.successful(HttpResponse(201, "error")))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Response body could not be parsed as JSON, body: error",
              None
            )
          )
        }
      }

      "routerConnector return an exception" in {
        val createRequest = createAPICreateRecordRequest()

        when(connector.createRecord(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.createRecord("GB123456789012", createRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Could not create record due to an internal error",
              None
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

            when(connector.createRecord(any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.createRecord("GB123456789012", createRequest)

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  "removeRecord" should {
    "return 200 OK " in {
      val httpResponse = HttpResponse(Status.OK, "")
      when(connector.removeRecord("GB123456789012", "recordId", "GB123456789012"))
        .thenReturn(Future.successful(httpResponse))

      val result = sut.removeRecord("GB123456789012", "recordId", "GB123456789012")

      whenReady(result) {
        _.value mustBe OK
      }
    }

    "return an error" when {

      "routerConnector return an exception" in {
        when(connector.removeRecord(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.removeRecord("eori", "recordId", "actorId")

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Could not remove record for eori number eori and record ID recordId",
              None
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
            when(connector.removeRecord(any, any, any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.removeRecord("eori", "recordId", "actorId")

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  "updateRecord" should {
    "update a record" in {
      val updateRequest = createUpdateRecordRequest()

      when(connector.updateRecord(any)(any))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(createResponse), Map.empty)))

      val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

      whenReady(result) { _ =>
        verify(connector).updateRecord(
          eqTo(RouterUpdateRecordRequest("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest))
        )(any)
      }
    }

    "return CreateOrUpdateRecordResponse" in {
      val updateRequest = createUpdateRecordRequest()

      when(connector.updateRecord(any)(any))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(createResponse), Map.empty)))

      val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

      whenReady(result)(_.value mustBe createResponse)
    }

    "return an error" when {
      "cannot parse the response" in {
        val updateRequest = createUpdateRecordRequest()

        when(connector.updateRecord(any)(any))
          .thenReturn(Future.successful(HttpResponse(200, Json.obj(), Map.empty)))

        val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Could not update record due to an internal error",
              None
            )
          )
        }
      }

      "cannot parse the response as Json" in {
        val updateRequest = createUpdateRecordRequest()

        when(connector.updateRecord(any)(any))
          .thenReturn(Future.successful(HttpResponse(200, "error")))

        val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Response body could not be parsed as JSON, body: error",
              None
            )
          )
        }
      }

      "routerConnector return an exception" in {
        val updateRequest = createUpdateRecordRequest()

        when(connector.updateRecord(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

        whenReady(result) {
          _.left.value mustBe ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse(
              "d677693e-9981-4ee3-8574-654981ebe606",
              "INTERNAL_SERVER_ERROR",
              "Could not update record due to an internal error",
              None
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
            val updateRequest = createUpdateRecordRequest()

            when(connector.updateRecord(any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result = sut.updateRecord("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", updateRequest)

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  "updateProfile" should {
    "update a profile" in {
      val updateRequest  = MaintainProfileRequest(
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )
      val updateResponse = UpdateProfileResponse(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = "RMS-GB-123456",
        niphlNumber = "6 S12345"
      )

      when(connector.routerMaintainProfile(any[String], any[MaintainProfileRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(200, Json.toJson(updateResponse), Map.empty)))

      val result = sut.updateProfile("GB123456789012", updateRequest)

      whenReady(result) { res =>
        res mustBe Right(updateResponse)
        verify(connector).routerMaintainProfile(
          eqTo("GB123456789012"),
          eqTo(updateRequest)
        )(any[HeaderCarrier])
      }
    }

    "return an error when the response cannot be parsed as JSON" in {
      val updateRequest = MaintainProfileRequest(
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(connector.routerMaintainProfile(any[String], any[MaintainProfileRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(200, "error")))

      val result = sut.updateProfile("GB123456789012", updateRequest)

      whenReady(result) {
        _.left.value mustBe ServiceError(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            "INTERNAL_SERVER_ERROR",
            "Response body could not be parsed as JSON, body: error"
          )
        )
      }
    }

    "return an error when the routerConnector returns an exception" in {
      val updateRequest = MaintainProfileRequest(
        actorId = "GB987654321098",
        ukimsNumber = "XIUKIM47699357400020231115081800",
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(connector.routerMaintainProfile(any[String], any[MaintainProfileRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("error")))

      val result = sut.updateProfile("GB123456789012", updateRequest)

      whenReady(result) {
        _.left.value mustBe ServiceError(
          INTERNAL_SERVER_ERROR,
          ErrorResponse(
            correlationId,
            "INTERNAL_SERVER_ERROR",
            "Could not update profile due to an internal error"
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

    forAll(table) { (description: String, status: Int, expectedResult: Int, code: String) =>
      s"$description" in {
        val updateRequest = MaintainProfileRequest(
          actorId = "GB987654321098",
          ukimsNumber = "XIUKIM47699357400020231115081800",
          nirmsNumber = Some("RMS-GB-123456"),
          niphlNumber = Some("6 S12345")
        )

        when(connector.routerMaintainProfile(any[String], any[MaintainProfileRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(createHttpResponse(status, code)))

        val result = sut.updateProfile("GB123456789012", updateRequest)

        whenReady(result) {
          _.left.value.status mustBe expectedResult
        }
      }
    }
  }

  "requestAccreditation" should {
    "request accreditation" in {
      val requestAccreditationRequest = createRequestAccreditationRequest()

      val httpResponse = HttpResponse(Status.CREATED, "")
      when(connector.requestAccreditation(any)(any))
        .thenReturn(Future.successful(httpResponse))

      val result =
        sut.requestAccreditation("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", requestAccreditationRequest)

      whenReady(result) { _ =>
        verify(connector).requestAccreditation(
          eqTo(
            RouterRequestAccreditationRequest(
              "GB123456789012",
              "d677693e-9981-4ee3-8574-654981ebe606",
              requestAccreditationRequest
            )
          )
        )(any)
      }
    }

    "return an error" when {

      "routerConnector return an exception" in {
        val requestAccreditationRequest = createRequestAccreditationRequest()

        when(connector.requestAccreditation(any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result =
          sut.requestAccreditation(
            "GB123456789012",
            "d677693e-9981-4ee3-8574-654981ebe606",
            requestAccreditationRequest
          )

        whenReady(result) {
          _.left.value mustBe ServiceError(
            500,
            ErrorResponse(
              correlationId,
              "INTERNAL_SERVER_ERROR",
              "Could not request accreditation due to an internal error",
              None
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
            val requestAccreditationRequest = createRequestAccreditationRequest()

            when(connector.requestAccreditation(any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result =
              sut.requestAccreditation(
                "GB123456789012",
                "d677693e-9981-4ee3-8574-654981ebe606",
                requestAccreditationRequest
              )

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  def createRequestAccreditationRequest(): RequestAccreditationRequest = RequestAccreditationRequest(
    actorId = "XI123456789001",
    requestorName = "Mr.Phil Edwards",
    requestorEmail = "Phil.Edwards@gmail.com"
  )

  private def createHttpResponse(status: Int, code: String) =
    HttpResponse(status, Json.toJson(ErrorResponse("correlationId", code, "any message")), Map.empty)
}

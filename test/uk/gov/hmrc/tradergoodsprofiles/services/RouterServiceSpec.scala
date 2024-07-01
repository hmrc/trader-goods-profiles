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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.tradergoodsprofiles.connectors.RouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.{RouterCreateRecordRequestSupport, UpdateRecordRequestSupport}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.{CreateOrUpdateRecordResponseSupport, GetRecordResponseSupport}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.responses.MaintainProfileResponse

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class RouterServiceSpec
    extends PlaySpec
    with GetRecordResponseSupport
    with CreateOrUpdateRecordResponseSupport
    with RouterCreateRecordRequestSupport
    with UpdateRecordRequestSupport
    with ScalaFutures
    with EitherValues
    with IntegrationPatience
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  private val connector      = mock[RouterConnector]
  private val createResponse = createCreateOrUpdateRecordResponse("recordId", "GB123456789012", Instant.now)
  private val uuidService    = mock[UuidService]
  private val correlationId  = "d677693e-9981-4ee3-8574-654981ebe606"

  private val eori     = "GB123456789012"
  private val recordId = "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f"

  private val sut = new RouterService(connector, uuidService)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(connector, uuidService)

    when(uuidService.uuid).thenReturn(correlationId)
  }

  "updateProfile" should {
    "update a profile" in {
      val updateProfileRequest: JsValue = Json
        .parse("""
                 |{
                 |    "actorId": "GB987654321098",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345"
                 |}
                 |""".stripMargin)

      def updateJsonRequest: Request[JsValue] =
        FakeRequest().withBody(updateProfileRequest)
      val updateRequest: Request[JsValue]     = updateJsonRequest

      val updateResponse = MaintainProfileResponse(
        eori = "GB123456789012",
        actorId = "GB987654321098",
        ukimsNumber = Some("XIUKIM47699357400020231115081800"),
        nirmsNumber = Some("RMS-GB-123456"),
        niphlNumber = Some("6 S12345")
      )

      when(connector.routerMaintainProfile(any[String], any[Request[JsValue]])(any[HeaderCarrier]))
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
      val updateProfileRequest: JsValue = Json
        .parse("""
                 |{
                 |    "actorId": "GB987654321098",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345"
                 |}
                 |""".stripMargin)

      def updateJsonRequest: Request[JsValue] =
        FakeRequest().withBody(updateProfileRequest)
      val updateRequest: Request[JsValue]     = updateJsonRequest
      when(connector.routerMaintainProfile(any[String], any[Request[JsValue]])(any[HeaderCarrier]))
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
      val updateProfileRequest: JsValue = Json
        .parse("""
                 |{
                 |    "actorId": "GB987654321098",
                 |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                 |    "nirmsNumber": "RMS-GB-123456",
                 |    "niphlNumber": "6 S12345"
                 |}
                 |""".stripMargin)

      def updateJsonRequest: Request[JsValue] =
        FakeRequest().withBody(updateProfileRequest)
      val updateRequest: Request[JsValue]     = updateJsonRequest

      when(connector.routerMaintainProfile(any[String], any[Request[JsValue]])(any[HeaderCarrier]))
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
        val updateProfileRequest: JsValue = Json
          .parse("""
                   |{
                   |    "actorId": "GB987654321098",
                   |    "ukimsNumber": "XIUKIM47699357400020231115081800",
                   |    "nirmsNumber": "RMS-GB-123456",
                   |    "niphlNumber": "6 S12345"
                   |}
                   |""".stripMargin)

        def updateJsonRequest: Request[JsValue] =
          FakeRequest().withBody(updateProfileRequest)
        val updateRequest: Request[JsValue]     = updateJsonRequest

        when(connector.routerMaintainProfile(any[String], any[Request[JsValue]])(any[HeaderCarrier]))
          .thenReturn(Future.successful(createHttpResponse(status, code)))

        val result = sut.updateProfile("GB123456789012", updateRequest)

        whenReady(result) {
          _.left.value.status mustBe expectedResult
        }
      }
    }
  }

  "requestAdvice" should {
    "request advice" in {

      val httpResponse = HttpResponse(Status.CREATED, "")
      when(connector.requestAdvice(any, any, any)(any))
        .thenReturn(Future.successful(httpResponse))

      val result =
        sut.requestAdvice("GB123456789012", "d677693e-9981-4ee3-8574-654981ebe606", createRequestAdviceRequest)

      whenReady(result) { _ =>
        verify(connector).requestAdvice(
          eqTo(
            createRequestAdviceRequest
          ),
          eqTo("GB123456789012"),
          eqTo("d677693e-9981-4ee3-8574-654981ebe606")
        )(any)
      }
    }

    "return an error" when {

      "routerConnector return an exception" in {

        when(connector.requestAdvice(any, any, any)(any))
          .thenReturn(Future.failed(new RuntimeException("error")))

        val result =
          sut.requestAdvice(
            "GB123456789012",
            "d677693e-9981-4ee3-8574-654981ebe606",
            createRequestAdviceRequest
          )

        whenReady(result) {
          _.left.value mustBe ServiceError(
            500,
            ErrorResponse(
              correlationId,
              "INTERNAL_SERVER_ERROR",
              "Could not request advice due to an internal error",
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

            when(connector.requestAdvice(any, any, any)(any))
              .thenReturn(Future.successful(createHttpResponse(status, code)))

            val result =
              sut.requestAdvice(eori, recordId, createRequestAdviceRequest)

            whenReady(result) {
              _.left.value.status mustBe expectedResult
            }
          }
      }
    }
  }

  val createRequestAdviceRequestData: JsValue = Json
    .parse("""
             |{
             |    "requestorName": "Mr.Phil Edwards",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |}
             |""".stripMargin)

  def requestAdviceJsonRequest: Request[JsValue]   =
    FakeRequest().withBody(createRequestAdviceRequestData)
  val createRequestAdviceRequest: Request[JsValue] = requestAdviceJsonRequest

  private def createHttpResponse(status: Int, code: String) =
    HttpResponse(status, Json.toJson(ErrorResponse("correlationId", code, "any message")), Map.empty)
}

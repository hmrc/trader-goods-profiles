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

package uk.gov.hmrc.tradergoodsprofiles.controllers

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RequestAdviceControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request       = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId      = UUID.randomUUID().toString
  private val correlationId = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService   = mock[UuidService]
  private val routerService = mock[RouterService]
  private val sut           = new RequestAdviceController(
    new FakeSuccessAuthAction(),
    routerService,
    uuidService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.requestAdvice(any, any, any)(any))
      .thenReturn(Future.successful(Right(CREATED)))
  }

  "requestAdvice" should {
    "return 201 when advice is successfully requested" in {
      val requestBody = createRequestAdviceRequest

      val result = sut.requestAdvice(eoriNumber, recordId)(request.withBody(Json.toJson(requestBody)))

      status(result) mustBe CREATED
    }

    "return 500 when the router service returns an error" in {
      val adviceRequest = createRequestAdviceRequest

      val expectedJson = Json.obj(
        "correlationId" -> correlationId,
        "code"          -> "INTERNAL_SERVER_ERROR",
        "message"       -> "Could not request advice due to an internal error"
      )

      val errorResponse =
        ErrorResponse.serverErrorResponse(uuidService.uuid, "Could not request advice due to an internal error")
      val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

      when(routerService.requestAdvice(any, any, any)(any))
        .thenReturn(Future.successful(Left(serviceError)))

      val result = sut.requestAdvice(eoriNumber, recordId)(request.withBody(Json.toJson(adviceRequest)))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expectedJson
    }
  }

  def createRequestAdviceRequest: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB9876543210983",
             |    "requestorName": "Mr.Phil Edwards",
             |    "requestorEmail": "Phil.Edwards@gmail.com"
             |
             |}
             |""".stripMargin)
}

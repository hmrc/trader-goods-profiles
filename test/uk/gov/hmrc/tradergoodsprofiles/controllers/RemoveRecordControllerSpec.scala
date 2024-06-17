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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests.RemoveRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.{RouterService, UuidService}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with BeforeAndAfterEach
    with RemoveRecordRequestSupport {

  implicit val ec: ExecutionContext = ExecutionContext.global

  lazy val removeRecordRequestData: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB098765432112"
             |}
             |""".stripMargin)

  private val request = FakeRequest()
    .withBody(removeRecordRequestData)
    .withHeaders(
      "Accept"       -> "application/vnd.hmrc.1.0+json",
      "Content-Type" -> "application/json",
      "X-Client-ID"  -> "some client ID"
    )

  val requestHeaders: Seq[(String, String)] = Seq(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json"
  )
  private val recordId                      = UUID.randomUUID().toString
  private val correlationId                 = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService                   = mock[UuidService]
  private val routerService                 = mock[RouterService]
  private val sut                           = new RemoveRecordController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(uuidService),
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, routerService)
    when(uuidService.uuid).thenReturn(correlationId)
    when(routerService.removeRecord(any, any, any)(any))
      .thenReturn(Future.successful(Right(OK)))
  }

  "removeRecord" should {
    "return 200" in {
      val result = sut.removeRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
    }

    "remove the record from router" in {
      val result = sut.removeRecord(eoriNumber, recordId)(request)

      status(result) mustBe OK
      verify(routerService).removeRecord(eqTo(eoriNumber), eqTo(recordId), eqTo(request))(any)
    }

    "return an error" when {

      "routerService return an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> "d677693e-9981-4ee3-8574-654981ebe606",
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"internal server error"
        )

        val errorResponse =
          ErrorResponse.serverErrorResponse(uuidService.uuid, "internal server error")
        val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

        when(routerService.removeRecord(any, any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.removeRecord(eoriNumber, recordId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }

}

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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.RemoveRecordRouterConnector
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.{AuthTestSupport, FakeUserAllowListAction}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RemoveRecordControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  /*
    TODO: remove for drop2 - TGP-2029
    The request should have no headers.
   */
  private val request = FakeRequest()
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
  private val actorId                       = "GB987654321098"
  private val correlationId                 = "d677693e-9981-4ee3-8574-654981ebe606"
  private val uuidService                   = mock[UuidService]
  private val connector                     = mock[RemoveRecordRouterConnector]
  private val appConfig                     = mock[AppConfig]
  private val sut                           = new RemoveRecordController(
    new FakeSuccessAuthAction(),
    new FakeUserAllowListAction(),
    connector,
    appConfig,
    uuidService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(uuidService, connector, appConfig)
    when(uuidService.uuid).thenReturn(correlationId)
    when(connector.removeRecord(any, any, any)(any))
      .thenReturn(Future.successful(Right(OK)))
    when(appConfig.sendClientId).thenReturn(false)
    when(appConfig.acceptHeaderDisabled).thenReturn(false)
  }

  "removeRecord" should {

    /*
    TODO: this test need to be removed for drop2 - TGP-2029
    The request should have no headers.
     */
    "return 204 when sendClientId feature flag is true" in {
      val result = sut.removeRecord(eoriNumber, recordId, actorId)(request)
      status(result) mustBe NO_CONTENT
    }

    "return 204 when sendClientId feature flag is false" in {
      when(appConfig.sendClientId).thenReturn(false)

      val result = sut.removeRecord(eoriNumber, recordId, actorId)(
        FakeRequest().withHeaders(
          "Accept" -> "application/vnd.hmrc.1.0+json"
        )
      )
      status(result) mustBe NO_CONTENT
    }

    "return 204 when acceptHeaderDisabled feature flag is true" in {
      when(appConfig.acceptHeaderDisabled).thenReturn(true)

      val result = sut.removeRecord(eoriNumber, recordId, actorId)(
        FakeRequest().withHeaders(
          "X-Client-ID" -> "some client ID"
        )
      )
      status(result) mustBe NO_CONTENT
    }

    "return 204 when acceptHeaderDisabled and sendClientId are true" in {
      when(appConfig.acceptHeaderDisabled).thenReturn(true)
      when(appConfig.sendClientId).thenReturn(true)

      val result = sut.removeRecord(eoriNumber, recordId, actorId)(
        FakeRequest().withHeaders(
          "X-Client-ID" -> "some client ID",
          "Accept"      -> "application/vnd.hmrc.1.0+json"
        )
      )
      status(result) mustBe NO_CONTENT
    }

    "remove the record from router" in {
      val result = sut.removeRecord(eoriNumber, recordId, actorId)(request)

      status(result) mustBe NO_CONTENT
      verify(connector).removeRecord(eqTo(eoriNumber), eqTo(recordId), eqTo(actorId))(any)
    }

    "return an error" when {

      "routerService returns an error" in {
        val expectedJson = Json.obj(
          "correlationId" -> "d677693e-9981-4ee3-8574-654981ebe606",
          "code"          -> "INTERNAL_SERVER_ERROR",
          "message"       -> s"internal server error"
        )

        val errorResponse =
          ErrorResponse.serverErrorResponse(uuidService.uuid, "internal server error")
        val serviceError  = ServiceError(INTERNAL_SERVER_ERROR, errorResponse)

        when(connector.removeRecord(any, any, any)(any))
          .thenReturn(Future.successful(Left(serviceError)))

        val result = sut.removeRecord(eoriNumber, recordId, actorId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe expectedJson
      }
    }
  }

}

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

import cats.data.EitherT
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.ValidateHeaderAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.request.APICreateRecordRequestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.response.CreateRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.services.{DateTimeService, RouterService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext

class CreateRecordControllerSpec
    extends PlaySpec
    with AuthTestSupport
    with CreateRecordResponseSupport
    with APICreateRecordRequestSupport
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  private val request         = FakeRequest().withHeaders(
    "Accept"       -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "X-Client-ID"  -> "some client ID"
  )
  private val recordId        = UUID.randomUUID().toString
  private val timestamp       = Instant.parse("2024-01-12T12:12:12Z")
  private val dateTimeService = mock[DateTimeService]
  private val routerService   = mock[RouterService]
  private val sut             = new CreateRecordController(
    new FakeSuccessAuthAction(),
    new ValidateHeaderAction(dateTimeService),
    dateTimeService,
    routerService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(dateTimeService, routerService)
    when(dateTimeService.timestamp).thenReturn(timestamp)
    when(routerService.createRecord(any, any)(any))
      .thenReturn(EitherT.fromEither(Right(createCreateRecordResponse(recordId, eoriNumber, timestamp))))
  }

  "createRecord" should {
    "return 201 when the record is successfully created" in {
      val createRequest = createCreateRecordRequest()

      val result = sut.createRecord(eoriNumber)(request.withBody(Json.toJson(createRequest)))

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(createCreateRecordResponse(recordId, eoriNumber, timestamp))
    }
  }
}

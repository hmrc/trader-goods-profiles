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

import org.mockito.MockitoSugar.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.time.Instant
import java.util.UUID

class GetRecordsControllerSpec extends PlaySpec with AuthTestSupport with BeforeAndAfterEach {

  private val timestamp       = Instant.parse("2024-01-12T12:12:12Z")
  private val dateTimeService = mock[DateTimeService]
  private val sut             = new GetRecordsController(
    new FakeSuccessAuthAction(),
    dateTimeService,
    stubControllerComponents()
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(dateTimeService)
    when(dateTimeService.timestamp).thenReturn(timestamp)
  }

  "getRecord" should {
    "return 200" in {
      val recordId = UUID.randomUUID().toString

      val result = sut.getRecord(eoriNumber, recordId)(FakeRequest())

      status(result) mustBe OK
    }

    "return an error" when {
      "eori is blank" in {
        val recordId = UUID.randomUUID().toString

        val result = sut.getRecord(" ", recordId)(FakeRequest())

        status(result) mustBe BAD_REQUEST
      }

      "eori is null" in {
        val recordId = UUID.randomUUID().toString

        val result = sut.getRecord(null, recordId)(FakeRequest())

        status(result) mustBe BAD_REQUEST
      }

      "eori is less than 14 characters" in {
        val recordId = UUID.randomUUID().toString

        val result = sut.getRecord("1234", recordId)(FakeRequest())

        status(result) mustBe BAD_REQUEST
      }

      "eori is greater than 17 characters" in {
        val recordId = UUID.randomUUID().toString

        val result = sut.getRecord("12341234123412341234", recordId)(FakeRequest())

        status(result) mustBe BAD_REQUEST
      }

      "recordId is not a UUID" in {
        val result = sut.getRecord(eoriNumber, "1234-abc")(FakeRequest())

        status(result) mustBe BAD_REQUEST
      }
    }
  }
}

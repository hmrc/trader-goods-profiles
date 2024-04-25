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

import org.scalatestplus.play.PlaySpec
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.AuthTestSupport
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.FakeAuth.FakeSuccessAuthAction

import java.util.UUID

class GetRecordsControllerSpec extends PlaySpec
 with AuthTestSupport {


  "getRecord" should {
    "return 200" in {

      val recordId = UUID.randomUUID().toString
      val sut = new GetRecordsController(new FakeSuccessAuthAction(), stubControllerComponents())

      val result = sut.getRecord(eoriNumber, recordId)(FakeRequest())

      status(result) mustBe OK

    }
  }
}

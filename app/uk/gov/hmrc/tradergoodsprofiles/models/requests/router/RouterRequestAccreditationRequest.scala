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

package uk.gov.hmrc.tradergoodsprofiles.models.requests.router

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.RequestAccreditationRequest

case class RouterRequestAccreditationRequest(
  eori: String,
  requestorName: String,
  recordId: String,
  requestorEmail: String
)

object RouterRequestAccreditationRequest {
  implicit val format: OFormat[RouterRequestAccreditationRequest] = Json.format[RouterRequestAccreditationRequest]

  def apply(
    eori: String,
    recordId: String,
    accreditationRequest: RequestAccreditationRequest
  ): RouterRequestAccreditationRequest =
    RouterRequestAccreditationRequest(
      eori = eori,
      requestorName = accreditationRequest.requestorName,
      recordId = recordId,
      requestorEmail = accreditationRequest.requestorEmail
    )
}

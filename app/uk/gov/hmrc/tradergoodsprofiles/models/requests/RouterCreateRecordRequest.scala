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

package uk.gov.hmrc.tradergoodsprofiles.models.requests

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment

import java.time.Instant

case class RouterCreateRecordRequest(
  eori: String,
  actorId: String,
  traderRef: String,
  comcode: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Int,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[Int] = None,
  measurementUnit: Option[String] = None,
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant] = None
)

object RouterCreateRecordRequest {
  implicit val format: OFormat[RouterCreateRecordRequest] = Json.format[RouterCreateRecordRequest]

  def apply(eori: String, createRequest: APICreateRecordRequest): RouterCreateRecordRequest =
    RouterCreateRecordRequest(
      eori = eori,
      actorId = createRequest.actorId,
      traderRef = createRequest.traderRef,
      comcode = createRequest.comcode,
      goodsDescription = createRequest.goodsDescription,
      countryOfOrigin = createRequest.countryOfOrigin,
      category = createRequest.category,
      assessments = createRequest.assessments,
      supplementaryUnit = createRequest.supplementaryUnit,
      measurementUnit = createRequest.measurementUnit,
      comcodeEffectiveFromDate = createRequest.comcodeEffectiveFromDate,
      comcodeEffectiveToDate = createRequest.comcodeEffectiveToDate
    )
}

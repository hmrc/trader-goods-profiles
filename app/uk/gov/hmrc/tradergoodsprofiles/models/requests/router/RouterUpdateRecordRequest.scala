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
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment
import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateRecordRequest

import java.time.Instant

case class RouterUpdateRecordRequest(
  eori: String,
  recordId: String,
  actorId: String,
  traderRef: Option[String] = None,
  comcode: Option[String] = None,
  goodsDescription: Option[String] = None,
  countryOfOrigin: Option[String] = None,
  category: Option[Int] = None,
  assessments: Option[Seq[Assessment]] = None,
  supplementaryUnit: Option[Int] = None,
  measurementUnit: Option[String] = None,
  comcodeEffectiveFromDate: Option[Instant] = None,
  comcodeEffectiveToDate: Option[Instant] = None
)

object RouterUpdateRecordRequest {
  implicit val format: OFormat[RouterUpdateRecordRequest] = Json.format[RouterUpdateRecordRequest]

  def apply(eori: String, recordId: String, updateRequest: UpdateRecordRequest): RouterUpdateRecordRequest =
    RouterUpdateRecordRequest(
      eori = eori,
      recordId = recordId,
      actorId = updateRequest.actorId,
      traderRef = updateRequest.traderRef,
      comcode = updateRequest.comcode,
      goodsDescription = updateRequest.goodsDescription,
      countryOfOrigin = updateRequest.countryOfOrigin,
      category = updateRequest.category,
      assessments = updateRequest.assessments,
      supplementaryUnit = updateRequest.supplementaryUnit,
      measurementUnit = updateRequest.measurementUnit,
      comcodeEffectiveFromDate = updateRequest.comcodeEffectiveFromDate,
      comcodeEffectiveToDate = updateRequest.comcodeEffectiveToDate
    )
}

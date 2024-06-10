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

package uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses

import uk.gov.hmrc.tradergoodsprofiles.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.models.{Assessment, Condition}

import java.time.Instant

trait CreateOrUpdateRecordResponseSupport {

  def createCreateOrUpdateRecordResponse(
    recordId: String,
    eori: String,
    timestamp: Instant
  ): CreateOrUpdateRecordResponse = {
    val condition  = Condition(
      Some("certificate"),
      Some("Y923"),
      Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
      Some("Excluded product")
    )
    val assessment = Assessment(Some("a06846e9a5f61fa4ecf2c4e3b23631fc"), Some(1), Some(condition))
    CreateOrUpdateRecordResponse(
      recordId = recordId,
      eori = eori,
      actorId = "GB987654321098",
      traderRef = "SKU123456",
      comcode = "123456",
      adviceStatus = "Not Requested",
      goodsDescription = "Bananas",
      countryOfOrigin = "GB",
      category = 2,
      assessments = Some(Seq(assessment)),
      supplementaryUnit = Some(13),
      measurementUnit = Some("Kilograms"),
      comcodeEffectiveFromDate = timestamp,
      comcodeEffectiveToDate = Some(timestamp),
      version = 1,
      active = true,
      toReview = false,
      reviewReason = Some("Commodity code changed"),
      declarable = "IMMI declarable",
      ukimsNumber = Some("XIUKIM47699357400020231115081800"),
      nirmsNumber = Some("RMS-GB-123456"),
      niphlNumber = Some("6 S12345"),
      createdDateTime = timestamp,
      updatedDateTime = timestamp
    )
  }
}

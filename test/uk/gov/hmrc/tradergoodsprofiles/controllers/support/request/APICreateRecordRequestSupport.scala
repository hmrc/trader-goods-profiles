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

package uk.gov.hmrc.tradergoodsprofiles.controllers.support.request

import uk.gov.hmrc.tradergoodsprofiles.models.{APICreateRecordRequest, Assessment, Condition}

import java.time.Instant

trait APICreateRecordRequestSupport {

  def createCreateRecordRequest(
    actorId: String = "GB987654321098",
    traderRef: String = "SKU123456",
    comcode: Int = 123456,
    goodsDescription: String = "Bananas",
    countryOfOrigin: String = "GB",
    category: Int = 2,
    supplementaryUnit: Int = 13,
    measurementUnit: String = "Kilograms",
    comcodeEffectiveFromDate: Instant = Instant.parse("2023-01-01T00:00:00Z"),
    comcodeEffectiveToDate: Instant = Instant.parse("2028-01-01T00:00:00Z")
  ): APICreateRecordRequest = {
    val condition  = Condition(
      "certificate",
      "Y923",
      "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
      "Excluded product"
    )
    val assessment = Assessment("a06846e9a5f61fa4ecf2c4e3b23631fc", 1, condition)
    APICreateRecordRequest(
      actorId,
      traderRef,
      comcode,
      goodsDescription,
      countryOfOrigin,
      category,
      Seq(assessment),
      supplementaryUnit,
      measurementUnit,
      comcodeEffectiveFromDate,
      comcodeEffectiveToDate
    )
  }
}

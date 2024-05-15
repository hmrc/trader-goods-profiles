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

package uk.gov.hmrc.tradergoodsprofiles.controllers.support

import uk.gov.hmrc.tradergoodsprofiles.models.{Assessment, Condition, GetRecordResponse}

import java.time.Instant

trait GetRecordResponseSupport {

  def createGetRecordResponse(
    eori: String,
    recordId: String,
    timestamp: Instant
  ): GetRecordResponse = {
    val condition  = Condition(
      "certificate",
      "Y923",
      "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
      "Excluded product"
    )
    val assessment = Assessment("a06846e9a5f61fa4ecf2c4e3b23631fc", 1, condition)
    GetRecordResponse(
      eori,
      "GB123456789012",
      recordId,
      "SKU123456",
      "123456",
      "Not Requested",
      "Bananas",
      "GB",
      2,
      Some(Seq(assessment)),
      Some(13),
      Some("Kilograms"),
      timestamp,
      Some(timestamp),
      1,
      true,
      false,
      Some("Commodity code changed"),
      "IMMI declarable",
      "XIUKIM47699357400020231115081800",
      "RMS-GB-123456",
      "6 S12345",
      false,
      "TGPS",
      timestamp,
      timestamp
    )
  }
}

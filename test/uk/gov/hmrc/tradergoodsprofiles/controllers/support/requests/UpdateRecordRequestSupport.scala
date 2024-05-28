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

package uk.gov.hmrc.tradergoodsprofiles.controllers.support.requests

import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.models.{Assessment, Condition}

import java.time.Instant

trait UpdateRecordRequestSupport {

  def createUpdateRecordRequest(
    actorId: String = "GB987654321098",
    traderRef: Option[String] = Some("SKU123456"),
    comcode: Option[String] = Some("123456"),
    goodsDescription: Option[String] = Some("Bananas"),
    countryOfOrigin: Option[String] = Some("GB"),
    category: Option[Int] = Some(2),
    assessments: Option[Seq[Assessment]] = Some(
      Seq(
        Assessment(
          Some("a06846e9a5f61fa4ecf2c4e3b23631fc"),
          Some(1),
          Some(
            Condition(
              Some("certificate"),
              Some("Y923"),
              Some("Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law"),
              Some("Excluded product")
            )
          )
        )
      )
    ),
    supplementaryUnit: Option[Int] = Some(13),
    measurementUnit: Option[String] = Some("Kilograms"),
    comcodeEffectiveFromDate: Option[Instant] = Some(Instant.parse("2023-01-01T00:00:00Z")),
    comcodeEffectiveToDate: Option[Instant] = Some(Instant.parse("2028-01-01T00:00:00Z"))
  ): UpdateRecordRequest =
    UpdateRecordRequest(
      actorId,
      traderRef,
      comcode,
      goodsDescription,
      countryOfOrigin,
      category,
      assessments,
      supplementaryUnit,
      measurementUnit,
      comcodeEffectiveFromDate,
      comcodeEffectiveToDate
    )
}

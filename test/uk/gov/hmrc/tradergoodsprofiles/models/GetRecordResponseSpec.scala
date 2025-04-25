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

package uk.gov.hmrc.tradergoodsprofiles.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsResult, Json}
import uk.gov.hmrc.tradergoodsprofiles.controllers.support.responses.GetRecordResponseSupport
import uk.gov.hmrc.tradergoodsprofiles.models.response.GetRecordResponse

import java.time.Instant

class GetRecordResponseSpec extends PlaySpec with GetRecordResponseSupport {

  private val timestamp         = Instant.parse("2023-01-01T00:00:00Z")
  private val getRecordResponse = createGetRecordResponse(
    "GB123456789012",
    "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    timestamp
  )

  private val getRecordResponseConverted = createGetRecordResponseForConverted(
    "GB123456789012",
    "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
    timestamp
  )

  "toJson" should {
    "convert Object to json" in {
      Json.toJson(getRecordResponseConverted) mustBe GetRecordResponseAsJson
    }
  }

  "fromJson" should {
    "convert json to object" in {
      val result: JsResult[GetRecordResponse] = GetRecordResponseAsJson.validate[GetRecordResponse]

      result.isSuccess mustBe true
      result.get mustBe getRecordResponse
    }
  }

  "return an error" when {
    "deserialising a join to object" in {
      val invalidGetRecordResponseJson = GetRecordResponseAsJson - "eori"

      invalidGetRecordResponseJson.validate[GetRecordResponse].isError mustBe true
    }
  }

  private def GetRecordResponseAsJson: JsObject =
    Json.obj(
      "eori"                     -> "GB123456789012",
      "actorId"                  -> "GB123456789012",
      "recordId"                 -> "8ebb6b04-6ab0-4fe2-ad62-e6389a8a204f",
      "traderRef"                -> "SKU123456",
      "comcode"                  -> "123456",
      "adviceStatus"             -> "Not Requested",
      "goodsDescription"         -> "Bananas",
      "countryOfOrigin"          -> "GB",
      "category"                 -> 2,
      "assessments"              -> Json.arr(
        Json.obj(
          "assessmentId"    -> "a06846e9a5f61fa4ecf2c4e3b23631fc",
          "primaryCategory" -> 1,
          "condition"       -> Json.obj(
            "type"                 -> "certificate",
            "conditionId"          -> "Y923",
            "conditionDescription" -> "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
            "conditionTraderText"  -> "Excluded product"
          )
        )
      ),
      "supplementaryUnit"        -> 13,
      "measurementUnit"          -> "Kilograms",
      "comcodeEffectiveFromDate" -> timestamp,
      "comcodeEffectiveToDate"   -> timestamp,
      "version"                  -> 1,
      "active"                   -> true,
      "toReview"                 -> true,
      "reviewReason"             -> "The commodity code has expired. You'll need to change the commodity code and categorise the goods.",
      "declarable"               -> "IMMI declarable",
      "ukimsNumber"              -> "XIUKIM47699357400020231115081800",
      "nirmsNumber"              -> "RMS-GB-123456",
      "niphlNumber"              -> "6 S12345",
      "locked"                   -> false,
      "createdDateTime"          -> timestamp,
      "updatedDateTime"          -> timestamp
    )

}

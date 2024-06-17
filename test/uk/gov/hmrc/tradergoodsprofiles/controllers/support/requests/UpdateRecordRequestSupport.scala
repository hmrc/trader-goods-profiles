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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest

trait UpdateRecordRequestSupport {

  def createUpdateRecordRequest: JsValue = Json
    .parse("""
             |{
             |    "actorId": "GB987654321098",
             |    "traderRef": "SKU123456",
             |    "comcode": "123456",
             |    "goodsDescription": "Bananas",
             |    "countryOfOrigin": "GB",
             |    "category": 2,
             |    "assessments": [
             |        {
             |            "assessmentId": "a06846e9a5f61fa4ecf2c4e3b23631fc",
             |            "primaryCategory": 1,
             |            "condition": {
             |                "type": "certificate",
             |                "conditionId": "Y923",
             |                "conditionDescription": "Products not considered as waste according to Regulation (EC) No 1013/2006 as retained in UK law",
             |                "conditionTraderText": "Excluded product"
             |            }
             |        }
             |    ],
             |    "supplementaryUnit": 13,
             |    "measurementUnit": "Kilograms",
             |    "comcodeEffectiveFromDate": "2024-11-18T23:20:19Z",
             |    "comcodeEffectiveToDate": "2024-11-18T23:20:19Z"
             |}
             |""".stripMargin)

  def updateJsonRequest: Request[JsValue] =
    FakeRequest().withBody(createUpdateRecordRequest)

}

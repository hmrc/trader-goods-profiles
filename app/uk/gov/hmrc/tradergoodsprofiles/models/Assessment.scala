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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Reads.verifying
import play.api.libs.json._

case class Assessment(assessmentId: Option[String], primaryCategory: Option[Int], condition: Option[Condition])

object Assessment {
  def nonEmptyString: Reads[String] = verifying[String](_.nonEmpty)

  implicit val reads: Reads[Assessment] = (
    (JsPath \ "assessmentId").readNullable[String](nonEmptyString) and
      (JsPath \ "primaryCategory").readNullable[Int] and
      (JsPath \ "condition").readNullable[Condition]
  )(Assessment.apply _)

  implicit val writes: OWrites[Assessment] = Json.writes[Assessment]

  implicit val format: OFormat[Assessment] = OFormat(reads, writes)
}

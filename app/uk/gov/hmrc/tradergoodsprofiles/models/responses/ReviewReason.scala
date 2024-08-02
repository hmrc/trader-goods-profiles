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

package uk.gov.hmrc.tradergoodsprofiles.models.responses

import enumeratum.{Enum, EnumEntry, PlayEnum}

sealed abstract class ReviewReason(val description: String) extends EnumEntry

object ReviewReason extends Enum[ReviewReason] with PlayEnum[ReviewReason] {
  val values: IndexedSeq[ReviewReason] = findValues

  case object mismatch
      extends ReviewReason(
        "HMRC have reviewed this record. The commodity code and goods description do not match. If you want to use this record on an IMMI, you'll need to amend the commodity code and the goods description."
      )
  case object inadequate
      extends ReviewReason(
        "HMRC have reviewed this record. The goods description does not have enough detail. If you want to use this record on an IMMI, you'll need to amend the goods description"
      )
  case object unclear
      extends ReviewReason(
        "HMRC have reviewed the record. The goods description is unclear. If you want to use this record on an IMMI, you'll need to amend the goods description."
      )
  case object commodity
      extends ReviewReason(
        "The commodity code has expired. You'll need to change the commodity code and categorise the goods."
      )
  case object measure
      extends ReviewReason(
        "The commodity code or restrictions have changed. You'll need to categorise the record."
      )

  def fromString(reason: String): Option[ReviewReason] = withNameOption(reason)
}

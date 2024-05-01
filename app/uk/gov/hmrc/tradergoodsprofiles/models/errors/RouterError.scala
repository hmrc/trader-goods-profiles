/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.tradergoodsprofiles.models.errors

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Reads, __}

sealed trait RouterError

object RouterError {

  case class UnexpectedError(thr: Option[Throwable] = None) extends RouterError
  case class GetFailedTGPError(message: String, code: ErrorCode) extends RouterError

}

object PresentationError {
  val MessageFieldName = "message"
  val CodeFieldName    = "code"

  implicit val standardErrorReads: Reads[StandardError] =
    (
      (__ \ MessageFieldName).read[String] and
        (__ \ CodeFieldName).read[ErrorCode]
    )(StandardError.apply _)

  sealed abstract class PresentationError extends Product with Serializable {
    def message: String

    def code: ErrorCode
  }

  case class StandardError(message: String, code: ErrorCode) extends PresentationError

}

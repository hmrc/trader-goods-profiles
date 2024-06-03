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

package uk.gov.hmrc.tradergoodsprofiles.utils

import play.api.libs.json.{JsPath, JsonValidationError}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.Error
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

object ValidationSupport {

  private val fieldOrder: Seq[String] = Seq(
    "/actorId",
    "/traderRef",
    "/comcode",
    "/goodsDescription",
    "/countryOfOrigin",
    "/category",
    "/assessments(0)/primaryCategory",
    "/assessments(0)/assessmentId",
    "/assessments(0)/condition/type",
    "/assessments(0)/condition/conditionId",
    "/assessments(0)/condition/conditionDescription",
    "/assessments(0)/condition/conditionTraderText",
    "/supplementaryUnit",
    "/measurementUnit",
    "/comcodeEffectiveFromDate",
    "/comcodeEffectiveToDate"
  )

  private val fieldsToErrorCode: Map[String, (String, String, Int)] = Map(
    "/actorId"                                       -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidActorMessage, ApplicationConstants.InvalidActorId),
    "/traderRef"                                     -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingTraderRef, ApplicationConstants.InvalidOrMissingTraderRefCode),
    "/comcode"                                       -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcode, ApplicationConstants.InvalidOrMissingComcodeCode),
    "/goodsDescription"                              -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingGoodsDescription, ApplicationConstants.InvalidOrMissingGoodsDescriptionCode),
    "/countryOfOrigin"                               -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCountryOfOrigin, ApplicationConstants.InvalidOrMissingCountryOfOriginCode),
    "/category"                                      -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCategory, ApplicationConstants.InvalidOrMissingCategoryCode),
    "/assessments(0)/primaryCategory"                -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategory, InvalidAssessmentPrimaryCategoryCode),
    "/assessments(0)/assessmentId"                   -> (ApplicationConstants.InvalidRequestParameter, InvalidOrMissingAssessmentId, InvalidOrMissingAssessmentIdCode),
    "/assessments(0)/condition/type"                 -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionType, InvalidAssessmentPrimaryCategoryConditionTypeCode),
    "/assessments(0)/condition/conditionId"          -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionId, InvalidAssessmentPrimaryCategoryConditionIdCode),
    "/assessments(0)/condition/conditionDescription" -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionDescription, InvalidAssessmentPrimaryCategoryConditionDescriptionCode),
    "/assessments(0)/condition/conditionTraderText"  -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionTraderText, InvalidAssessmentPrimaryCategoryConditionTraderTextCode),
    "/supplementaryUnit"                             -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingSupplementaryUnit, ApplicationConstants.InvalidOrMissingSupplementaryUnitCode),
    "/measurementUnit"                               -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingMeasurementUnit, ApplicationConstants.InvalidOrMissingMeasurementUnitCode),
    "/comcodeEffectiveFromDate"                      -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDateCode),
    "/comcodeEffectiveToDate"                        -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDateCode)
  )

  def convertError(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ): Seq[Error] =
    errors
      .flatMap { case (path, _) =>
        fieldsToErrorCode.get(path.toString).map { case (code, message, errorNumber) =>
          (fieldOrder.indexOf(path.toString), Error(code, message, errorNumber))
        }
      }
      .toSeq
      .sortBy(_._1)
      .map(_._2)
}

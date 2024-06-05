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

import play.api.libs.json._
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, ErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.UpdateRecordRequest
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants._

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object ValidationSupport {

  private val fieldOrder: Seq[String] = Seq(
    "/actorId",
    "/traderRef",
    "/comcode",
    "/goodsDescription",
    "/countryOfOrigin",
    "/category",
    "/assessments/assessmentId",
    "/assessments/primaryCategory",
    "/assessments/condition/type",
    "/assessments/condition/conditionId",
    "/assessments/condition/conditionDescription",
    "/assessments/condition/conditionTraderText",
    "/supplementaryUnit",
    "/measurementUnit",
    "/comcodeEffectiveFromDate",
    "/comcodeEffectiveToDate",
    "/ukimsNumber",
    "/nirmsNumber",
    "/niphlNumber"
  )

  private val mandatoryFieldsToErrorCode: Map[String, (String, String, Int)] = Map(
    "/actorId"                                    -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidActorMessage, ApplicationConstants.InvalidActorId),
    "/traderRef"                                  -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingTraderRef, ApplicationConstants.InvalidOrMissingTraderRefCode),
    "/comcode"                                    -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcode, ApplicationConstants.InvalidOrMissingComcodeCode),
    "/goodsDescription"                           -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingGoodsDescription, ApplicationConstants.InvalidOrMissingGoodsDescriptionCode),
    "/countryOfOrigin"                            -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCountryOfOrigin, ApplicationConstants.InvalidOrMissingCountryOfOriginCode),
    "/category"                                   -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCategory, ApplicationConstants.InvalidOrMissingCategoryCode),
    "/assessments/assessmentId"                   -> (ApplicationConstants.InvalidRequestParameter, InvalidOrMissingAssessmentId, InvalidOrMissingAssessmentIdCode),
    "/assessments/primaryCategory"                -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategory, InvalidAssessmentPrimaryCategoryCode),
    "/assessments/condition/type"                 -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionType, InvalidAssessmentPrimaryCategoryConditionTypeCode),
    "/assessments/condition/conditionId"          -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionId, InvalidAssessmentPrimaryCategoryConditionIdCode),
    "/assessments/condition/conditionDescription" -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionDescription, InvalidAssessmentPrimaryCategoryConditionDescriptionCode),
    "/assessments/condition/conditionTraderText"  -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionTraderText, InvalidAssessmentPrimaryCategoryConditionTraderTextCode),
    "/supplementaryUnit"                          -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingSupplementaryUnit, ApplicationConstants.InvalidOrMissingSupplementaryUnitCode),
    "/measurementUnit"                            -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingMeasurementUnit, ApplicationConstants.InvalidOrMissingMeasurementUnitCode),
    "/comcodeEffectiveFromDate"                   -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDateCode),
    "/comcodeEffectiveToDate"                     -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDateCode),
    "/ukimsNumber"                                -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingUkimsNumber, ApplicationConstants.InvalidOrMissingUkimsNumberCode),
    "/nirmsNumber"                                -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingNirmsNumber, ApplicationConstants.InvalidOrMissingNirmsNumberCode),
    "/niphlNumber"                                -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingNiphlNumber, ApplicationConstants.InvalidOrMissingNiphlNumberCode)
  )

  private val optionalFieldsToErrorCode: Map[String, (String, String, Int)] = Map(
    "/actorId"                                    -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidActorMessage, ApplicationConstants.InvalidActorId),
    "/traderRef"                                  -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalTraderRef, ApplicationConstants.InvalidOrMissingTraderRefCode),
    "/comcode"                                    -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalComcode, ApplicationConstants.InvalidOrMissingComcodeCode),
    "/goodsDescription"                           -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalGoodsDescription, ApplicationConstants.InvalidOrMissingGoodsDescriptionCode),
    "/countryOfOrigin"                            -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalCountryOfOrigin, ApplicationConstants.InvalidOrMissingCountryOfOriginCode),
    "/category"                                   -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalCategory, ApplicationConstants.InvalidOrMissingCategoryCode),
    "/assessments/assessmentId"                   -> (ApplicationConstants.InvalidRequestParameter, InvalidOrMissingAssessmentId, InvalidOrMissingAssessmentIdCode),
    "/assessments/primaryCategory"                -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategory, InvalidAssessmentPrimaryCategoryCode),
    "/assessments/condition/type"                 -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionType, InvalidAssessmentPrimaryCategoryConditionTypeCode),
    "/assessments/condition/conditionId"          -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionId, InvalidAssessmentPrimaryCategoryConditionIdCode),
    "/assessments/condition/conditionDescription" -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionDescription, InvalidAssessmentPrimaryCategoryConditionDescriptionCode),
    "/assessments/condition/conditionTraderText"  -> (ApplicationConstants.InvalidRequestParameter, InvalidAssessmentPrimaryCategoryConditionTraderText, InvalidAssessmentPrimaryCategoryConditionTraderTextCode),
    "/supplementaryUnit"                          -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingSupplementaryUnit, ApplicationConstants.InvalidOrMissingSupplementaryUnitCode),
    "/measurementUnit"                            -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingMeasurementUnit, ApplicationConstants.InvalidOrMissingMeasurementUnitCode),
    "/comcodeEffectiveFromDate"                   -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingOptionalComcodeEffectiveFromDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDateCode),
    "/comcodeEffectiveToDate"                     -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveToDateCode)
  )

  def convertError[T](
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  )(implicit tt: TypeTag[T]): Seq[Error] =
    extractSimplePaths(errors)
      .flatMap { case key =>
        tt.tpe match {
          case t if t =:= typeOf[UpdateRecordRequest] =>
            optionalFieldsToErrorCode.get(key).map { case (code, message, errorNumber) =>
              (fieldOrder.indexOf(key), Error(code, message, errorNumber))
            }
          case _                                      =>
            mandatoryFieldsToErrorCode.get(key).map { case (code, message, errorNumber) =>
              (fieldOrder.indexOf(key), Error(code, message, errorNumber))
            }
        }
      }
      .toSeq
      .sortBy(_._1)
      .map(_._2)

  private def extractSimplePaths(
    errors: scala.collection.Seq[(JsPath, collection.Seq[JsonValidationError])]
  ): collection.Seq[String] =
    errors
      .map(_._1)
      .map(_.path.filter(_.isInstanceOf[KeyPathNode]))
      .map(_.mkString)

  def validateRequestBody[T](
    json: JsValue,
    uuidService: UuidService
  )(implicit reads: Reads[T], tt: TypeTag[T]): Either[ErrorResponse, T] =
    json.validate[T] match {
      case JsSuccess(request, _) =>
        Right(request)
      case JsError(errors)       =>
        Left(
          ErrorResponse.badRequestErrorResponse(uuidService.uuid, Some(convertError(errors)))
        )
    }
}

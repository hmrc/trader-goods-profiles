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

import play.api.libs.json.{JsPath, JsValue, JsonValidationError}
import play.api.mvc.Result
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{Error, InvalidErrorsResponse}
import uk.gov.hmrc.tradergoodsprofiles.models.requests.APICreateRecordRequest

import scala.concurrent.{ExecutionContext, Future}

object ValidationSupport {

  private val fieldsToErrorCode: Map[String, (String, String, Int)] = Map(
    "/actorId"                  -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidActorMessage, ApplicationConstants.InvalidActorId),
    "/traderRef"                -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingTraderRef, ApplicationConstants.InvalidOrMissingTraderRefCode),
    "/comcode"                  -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcode, ApplicationConstants.InvalidOrMissingComcodeCode),
    "/goodsDescription"         -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingGoodsDescription, ApplicationConstants.InvalidOrMissingGoodsDescriptionCode),
    "/countryOfOrigin"          -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCountryOfOrigin, ApplicationConstants.InvalidOrMissingCountryOfOriginCode),
    "/category"                 -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingCategory, ApplicationConstants.InvalidOrMissingCategoryCode),
    "/comcodeEffectiveFromDate" -> (ApplicationConstants.InvalidRequestParameter, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate, ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDateCode)
  )

  def validateCreateRecordRequest(json: JsValue, correlationId: String)(implicit
    ec: ExecutionContext
  ): Future[Either[Result, APICreateRecordRequest]] =
    json.validate[APICreateRecordRequest].asEither match {
      case Right(request)         =>
        val errors = validateFields(request)
        if (errors.isEmpty) {
          Future.successful(Right(request))
        } else {
          Future.successful(Left(constructErrorResponse(correlationId, errors)))
        }
      case Left(validationErrors) =>
        Future.successful(Left(constructErrorResponse(correlationId, convertError(validationErrors))))
    }

  private def validateFields(request: APICreateRecordRequest): Seq[Error] = {
    val validations = List(
      (request.actorId.isEmpty, ApplicationConstants.InvalidActorMessage, ApplicationConstants.InvalidActorId),
      (
        request.traderRef.isEmpty,
        ApplicationConstants.InvalidOrMissingTraderRef,
        ApplicationConstants.InvalidOrMissingTraderRefCode
      ),
      (
        request.comcode.isEmpty,
        ApplicationConstants.InvalidOrMissingComcode,
        ApplicationConstants.InvalidOrMissingComcodeCode
      ),
      (
        request.goodsDescription.isEmpty,
        ApplicationConstants.InvalidOrMissingGoodsDescription,
        ApplicationConstants.InvalidOrMissingGoodsDescriptionCode
      ),
      (
        request.countryOfOrigin.isEmpty,
        ApplicationConstants.InvalidOrMissingCountryOfOrigin,
        ApplicationConstants.InvalidOrMissingCountryOfOriginCode
      ),
      (
        request.category == 0,
        ApplicationConstants.InvalidOrMissingCategory,
        ApplicationConstants.InvalidOrMissingCategoryCode
      ),
      (
        request.comcodeEffectiveFromDate.toString.isEmpty,
        ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDate,
        ApplicationConstants.InvalidOrMissingComcodeEffectiveFromDateCode
      )
    )

    validations.collect { case (true, message, errorNumber) =>
      Error(ApplicationConstants.InvalidRequestParameter, message, errorNumber)
    }
  }

  private def constructErrorResponse(correlationId: String, errors: Seq[Error]): Result = {
    val defaultError = Error(
      code = ApplicationConstants.InvalidRequestParameter,
      message = ApplicationConstants.InvalidJsonMessage,
      errorNumber = ApplicationConstants.InvalidJson
    )

    InvalidErrorsResponse(
      correlationId,
      errors = Some(if (errors.isEmpty) Seq(defaultError) else errors)
    ).toResult
  }

  def convertError(
    errors: scala.collection.Seq[(JsPath, scala.collection.Seq[JsonValidationError])]
  ): Seq[Error] =
    errors.flatMap { case (path, validationErrors) =>
      fieldsToErrorCode.get(path.toJsonString).map { case (code, message, errorNumber) =>
        Error(code, message, errorNumber)
      }
    }.toSeq
}

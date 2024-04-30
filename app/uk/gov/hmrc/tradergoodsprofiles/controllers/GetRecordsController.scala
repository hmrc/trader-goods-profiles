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

package uk.gov.hmrc.tradergoodsprofiles.controllers

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.AuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.InvalidRecordIdErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class GetRecordsController @Inject() (
  authAction: AuthAction,
  dateTimeService: DateTimeService,
  cc: ControllerComponents
) extends BackendController(cc) {

  def getRecord(eori: String, recordId: String): Action[AnyContent] =
    authAction(eori).async { implicit request =>
      Try(UUID.fromString(recordId)) match {
        case Success(value) =>
          Future.successful(Ok("Good job, you have been successfully authenticate. Under Implementation"))
        case Failure(_)     =>
          Future.successful(
            InvalidRecordIdErrorResponse(
              dateTimeService.timestamp,
              s"Invalid record ID supplied for eori $eori"
            ).toResult
          )
      }
    }

}

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

package uk.gov.hmrc.tradergoodsprofiles.controllers.actions

import com.google.inject.ImplementedBy
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.connectors.UserAllowListConnector
import uk.gov.hmrc.tradergoodsprofiles.models.UserRequest
import uk.gov.hmrc.tradergoodsprofiles.models.errors.UserNotAllowedResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAllowListActionImpl @Inject() (
  userAllowListConnector: UserAllowListConnector,
  uuidService: UuidService,
  appConfig: AppConfig
)(implicit
  val executionContext: ExecutionContext
) extends UserAllowListAction
    with Logging {

  override def refine[A](request: UserRequest[A]): Future[Either[Result, UserRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    if (appConfig.userAllowListEnabled) {
      userAllowListConnector
        .check("private-beta", request.eori)
        .flatMap {
          case false =>
            Future.successful(
              Left(
                UserNotAllowedResponse(
                  uuidService.uuid,
                  "This service is in private beta and not available to the public. We will aim to open the service to the public soon."
                ).toResult
              )
            )
          case true  => Future.successful(Right(request))
        } recoverWith { case e: Exception =>
        logger.warn(
          s"[UserAllowListAction] - Exception when checking if user was on the allow list",
          e
        )
        Future.failed(e)
      }
    } else {
      logger.info("user allow list feature flag is disabled, always returning successfully")
      Future.successful(Right(request))
    }
  }
}

@ImplementedBy(classOf[UserAllowListActionImpl])
trait UserAllowListAction extends ActionRefiner[UserRequest, UserRequest]

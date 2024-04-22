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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofiles.models.ErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.models.auth.EnrolmentRequest
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import javax.inject.Inject
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}


class AuthActionImpl @Inject()
(
  override val authConnector: AuthConnector,
  dateTimeService: DateTimeService,
  cc: ControllerComponents,
  val parser: BodyParsers.Default
)
(implicit val ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions with AuthAction with Logging {

  private val fetch = authorisedEnrolments and affinityGroup

  protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](
    request: Request[A],
    block: EnrolmentRequest[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    implicit val req: Request[A] = request

    authorised(Enrolment("HMRC-CUS-ORG"))
      .retrieve(fetch) {
        case authorisedEnrolments ~ Some(Organisation) => block(EnrolmentRequest(request))
        case _ ~ Some(Agent) =>
          successful(handleUnauthorisedError("Could not retrieve affinity group from Auth"))

      }.recover {
      case error: AuthorisationException => handleUnauthorisedError(error.reason)

      case ex: Throwable =>
        logger.error(s"Internal server error for ${request.uri} with error ${ex.getMessage}", ex)

        InternalServerError(Json.toJson(ErrorResponse(
          dateTimeService.timestamp,
          "Internal server error",
          s"Internal server error for ${request.uri} with error ${ex.getMessage}"))
        )
    }
  }

  private def handleUnauthorisedError[A](
    errorMessage: String
  )(implicit request: Request[A]): Result = {

    logger.error(s"Unauthorised error for ${request.uri} with error $errorMessage")

    Unauthorized(Json.toJson(ErrorResponse(
      dateTimeService.timestamp,
      "Unauthorised",
      s"Unauthorised error for ${request.uri} with error: $errorMessage")
    ))
  }
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[EnrolmentRequest, AnyContent] with ActionFunction[Request, EnrolmentRequest]

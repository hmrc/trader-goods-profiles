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
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.AuthAction.gtpEnrolmentKey
import uk.gov.hmrc.tradergoodsprofiles.models.ErrorResponse
import uk.gov.hmrc.tradergoodsprofiles.models.auth.EnrolmentRequest
import uk.gov.hmrc.tradergoodsprofiles.services.DateTimeService

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionImpl @Inject()
(
  override val authConnector: AuthConnector,
  dateTimeService: DateTimeService,
  appConfig: AppConfig,
  val bodyParser: BodyParsers.Default,
  cc: ControllerComponents,
  val parser: BodyParsers.Default
)
(implicit val ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions with AuthAction with Logging {

  private val fetch = authorisedEnrolments and affinityGroup



  override def apply(eori: String): ActionBuilder[EnrolmentRequest, AnyContent] with ActionFunction[Request, EnrolmentRequest] =
    new ActionBuilder[EnrolmentRequest, AnyContent] with ActionFunction[Request, EnrolmentRequest] {

      override val parser = bodyParser
      protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](
                                   request: Request[A],
                                   block: EnrolmentRequest[A] => Future[Result]
                                 ): Future[Result] = {

        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
        implicit val req: Request[A] = request

        authorised(Enrolment(gtpEnrolmentKey))
          .retrieve(fetch) {
            case authorisedEnrolments ~ Some(affinityGroup) if affinityGroup != Agent =>
              validateIdentifier(eori, authorisedEnrolments, block)
            case _ ~ Some(Agent) =>
              successful(handleUnauthorisedError(s"Invalid affinity group Agent from Auth"))
            case _ =>
              successful(handleUnauthorisedError("Invalid enrolment parameter from Auth"))

          }.recover {
          case error: AuthorisationException => handleUnauthorisedError(error.reason)

          case ex: Throwable =>
            logger.error(s"Internal server error for ${request.uri} with error ${ex.getMessage}", ex)

            InternalServerError(Json.toJson(ErrorResponse(
              dateTimeService.timestamp,
              "INTERNAL_SERVER_ERROR",
              s"Internal server error for ${request.uri} with error: ${ex.getMessage}"))
            )
        }
      }
    }


  private def validateIdentifier[A](
    eroiNumber: String,
    authorisedEnrolments: Enrolments,
    block: EnrolmentRequest[A] => Future[Result]
  )(implicit request: Request[A]): Future[Result] = {

    getIdentifierForGtpEnrolment(authorisedEnrolments) match {
      case Some(eroi) if eroi.value == eroiNumber  => block(EnrolmentRequest(request))
      case _ => Future.successful(Forbidden(Json.toJson(ErrorResponse(
        dateTimeService.timestamp,
        "FORBIDDEN",
        s"Supplied OAuth token not authorised to access data for given identifier(s) $eroiNumber"
      ))))
    }
  }

  private def getIdentifierForGtpEnrolment[A](enrolments: Enrolments): Option[EnrolmentIdentifier] = {
    val t = enrolments
      .getEnrolment(gtpEnrolmentKey)

      val o = t.fold[Option[EnrolmentIdentifier]](None)(
        e => e.getIdentifier(appConfig.tgpIdentifier)
      )
    o
  }

  private def handleUnauthorisedError[A](
    errorMessage: String
  )(implicit request: Request[A]): Result = {

    logger.error(s"Unauthorised error for ${request.uri} with error $errorMessage")

    Unauthorized(Json.toJson(ErrorResponse(
      dateTimeService.timestamp,
      "UNAUTHORIZED",
      s"Unauthorised error for ${request.uri} with error: $errorMessage")
    ))
  }
}


object AuthAction  {
  val gtpEnrolmentKey = "HMRC-CUS-ORG"
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction {
  def apply(ern: String): ActionBuilder[EnrolmentRequest, AnyContent] with ActionFunction[Request, EnrolmentRequest]
}

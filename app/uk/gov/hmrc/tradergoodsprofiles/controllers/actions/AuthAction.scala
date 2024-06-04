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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.AuthAction.{gtpEnrolmentKey, gtpIdentifierKey}
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ForbiddenErrorResponse, ServerErrorResponse, UnauthorisedErrorResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  uuidService: UuidService,
  val bodyParser: BodyParsers.Default,
  cc: ControllerComponents
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with AuthAction
    with Logging {

  private val fetch = authorisedEnrolments and affinityGroup

  override def apply(
    eori: String
  ): ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] =
    new ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request] {

      override val parser                              = bodyParser
      protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](
        request: Request[A],
        block: Request[A] => Future[Result]
      ): Future[Result] = {

        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
        implicit val req: Request[A]   = request

        authorised(Enrolment(gtpEnrolmentKey))
          .retrieve(fetch) {
            case authorisedEnrolments ~ Some(affinityGroup) if affinityGroup != Agent =>
              validateIdentifier(eori, authorisedEnrolments, block)
            case _ ~ Some(Agent)                                                      =>
              successful(
                handleInvalidAffinityGroup(
                  s"Affinity group 'agent' is not supported. Affinity group needs to be 'individual' or 'organisation'"
                )
              )
            case _                                                                    =>
              successful(
                handleInvalidAffinityGroup(
                  "Empty affinity group is not supported. Affinity group needs to be 'individual' or 'organisation'"
                )
              )

          }
          .recover {
            case error: AuthorisationException => handleUnauthorisedError(error.reason)
            case ex: Throwable                 => handleException(request, ex)
          }
      }
    }

  private def handleException[A](request: Request[A], ex: Throwable): Result = {
    logger.error(s"Internal server error for ${request.uri} with error ${ex.getMessage}", ex)

    ServerErrorResponse(
      uuidService.uuid,
      s"Internal server error for ${request.uri} with error: ${ex.getMessage}"
    ).toResult
  }

  private def validateIdentifier[A](
    eoriNumber: String,
    authorisedEnrolments: Enrolments,
    block: Request[A] => Future[Result]
  )(implicit request: Request[A]): Future[Result] = {

    val eoriNumbers = getIdentifierForGtpEnrolment(authorisedEnrolments)

    if (eoriNumbers.contains(eoriNumber)) block(request)
    else handleForbiddenError(eoriNumber)
  }

  private def handleForbiddenError[A](eoriNumber: String)(implicit request: Request[A]): Future[Result] = {
    logger.error(s"Forbidden error for ${request.uri}, eori number $eoriNumber")

    Future.successful(
      ForbiddenErrorResponse(
        uuidService.uuid,
        s"EORI number is incorrect"
      ).toResult
    )
  }

  private def getIdentifierForGtpEnrolment[A](enrolments: Enrolments): Seq[String] =
    enrolments
      .getEnrolment(gtpEnrolmentKey)
      .fold[Seq[EnrolmentIdentifier]](Seq.empty)(e =>
        e.identifiers.filter(i => i.key.equalsIgnoreCase(gtpIdentifierKey))
      )
      .map(_.value)
      .distinct

  private def handleInvalidAffinityGroup[A](
    errorMessage: String
  )(implicit request: Request[A]): Result = {

    logger.error(s"Unauthorised exception for ${request.uri} with error $errorMessage")

    UnauthorisedErrorResponse(
      uuidService.uuid,
      errorMessage
    ).toResult
  }

  private def handleUnauthorisedError[A](
    errorMessage: String
  )(implicit request: Request[A]): Result = {

    logger.error(s"Unauthorised exception for ${request.uri} with error $errorMessage")

    UnauthorisedErrorResponse(
      uuidService.uuid,
      s"The details signed in do not have a Trader Goods Profile"
    ).toResult
  }
}

object AuthAction {
  val gtpEnrolmentKey  = "HMRC-CUS-ORG"
  val gtpIdentifierKey = "EORINumber"
}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction {
  def apply(eori: String): ActionBuilder[Request, AnyContent] with ActionFunction[Request, Request]
}

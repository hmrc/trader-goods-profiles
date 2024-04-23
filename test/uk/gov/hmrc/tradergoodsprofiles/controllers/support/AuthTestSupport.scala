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

package uk.gov.hmrc.tradergoodsprofiles.controllers.support

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, Enrolment, Enrolments}

import scala.concurrent.Future

trait AuthTestSupport {

  lazy val authConnector = mock[AuthConnector]

  private val authFetch = authorisedEnrolments and affinityGroup
  protected val tgpIdentifierName = "tgpIdentifier"
  protected val eroiNumber = "GB000000000123"
  private val enrolmentKey = "HMRC-CUS-ORG"
  private val enrolment = Enrolment(enrolmentKey).withIdentifier(tgpIdentifierName, eroiNumber)


  def withAuthorizedTrader: Unit = {
    val retrieval = Enrolments(Set(enrolment)) and
      Some(AffinityGroup.Organisation)

    withAuthorization(retrieval)
  }

  def withAuthorization(retrieval: Enrolments ~ Option[AffinityGroup]): Unit = {

    when(authConnector.authorise(ArgumentMatchers.argThat((p: Predicate) => true), eqTo(authFetch))(any,any))
      .thenReturn(Future.successful(retrieval))
  }

  def authorizeWithAffinityGroup(affinityGrp: Option[AffinityGroup]): Unit = {
    val retrieval = Enrolments(Set(enrolment)) and affinityGrp

    withAuthorization(retrieval)
  }

  def withUnauthorizedTrader(error: Throwable): Unit =
    when(authConnector.authorise(any, any)(any, any)).thenReturn(Future.failed(error))

  def withUnauthorizedEmptyIdentifier: Unit = {
    val retrieval = Enrolments(Set(Enrolment(enrolmentKey))) and
      Some(AffinityGroup.Organisation)

    withAuthorization(retrieval)
  }

}

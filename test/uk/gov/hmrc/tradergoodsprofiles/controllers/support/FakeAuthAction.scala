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

import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc._
import uk.gov.hmrc.tradergoodsprofiles.controllers.actions.AuthAction
import uk.gov.hmrc.tradergoodsprofiles.models.UserRequest

import scala.concurrent.{ExecutionContext, Future}

object FakeAuth {
  class FakeSuccessAuthAction extends AuthAction {
    override def apply(
      eori: String
    ): ActionBuilder[UserRequest, AnyContent] with ActionFunction[Request, UserRequest] =
      new ActionBuilder[UserRequest, AnyContent] with ActionFunction[Request, UserRequest] {

        override val parser: BodyParsers.Default         = mock[BodyParsers.Default]
        protected def executionContext: ExecutionContext = ExecutionContext.global

        override def invokeBlock[A](
          request: Request[A],
          block: UserRequest[A] => Future[Result]
        ): Future[Result] =
          block(UserRequest(request, eori))
      }
  }
}

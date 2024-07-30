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

package uk.gov.hmrc.tradergoodsprofiles.support

import io.lemonlabs.uri.Url
import org.mockito.MockitoSugar.when
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import play.api.http.MimeTypes
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.XClientIdHeader

import java.util.UUID
import scala.concurrent.ExecutionContext

class BaseConnectorSpec extends PlaySpec {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders =
    Seq(
      XClientIdHeader          -> "clientId",
      HeaderNames.ACCEPT       -> "application/vnd.hmrc.1.0+json",
      HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
    )
  )

  protected val httpClient: HttpClientV2       = mock[HttpClientV2]
  protected val appConfig: AppConfig           = mock[AppConfig]
  protected val requestBuilder: RequestBuilder = mock[RequestBuilder]
  protected val uuidService: UuidService       = mock[UuidService]
  protected val correlationId: String          = UUID.randomUUID().toString
  protected val serverUrl: String              = "http://localhost:23123"

  def commonSetUp: ScalaOngoingStubbing[String] = {
    when(appConfig.routerUrl).thenReturn(Url.parse(serverUrl))
    when(uuidService.uuid).thenReturn(correlationId)
  }

}

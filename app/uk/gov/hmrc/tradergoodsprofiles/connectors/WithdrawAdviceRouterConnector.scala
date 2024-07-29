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

package uk.gov.hmrc.tradergoodsprofiles.connectors

import io.lemonlabs.uri.UrlPath
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WithdrawAdviceRouterConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with RouterHttpReader
    with Logging {

  def withdrawAdvice(eori: String, recordId: String, request: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, Int]] = {

    val url = appConfig.routerUrl.withPath(routerWithdrawAdviceUrl(eori, recordId))

    httpClient
      .put(url"$url")
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .setHeader(HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json")
      .withBody(Json.toJson(request.body))
      .withClientId
      .execute(httpReaderWithoutResponseBody, ec)
      .recover { case ex: Throwable =>
        logger.warn(
          s"[WithdrawAdviceRouterConnector] - Exception when withdraw advice  for eori number $eori, record ID $recordId, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                "Could not withdraw advice due to an internal error"
              )
          )
        )
      }
  }

  private def routerWithdrawAdviceUrl(eori: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eori/records/$recordId/advice"
    )
}

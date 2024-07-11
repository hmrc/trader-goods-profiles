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

import io.lemonlabs.uri.{QueryString, Url, UrlPath}
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
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

  def withdrawAdvice(eori: String, recordId: String, withdrawReason: Option[String])(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, Int]] = {

    val url = routerWithdrawAdviceUrl(eori, recordId, withdrawReason)
    httpClient
      .delete(url"$url")
      .withClientId
      .execute(httpReaderWithoutResponseBody, ec)
      .recover { case ex: Throwable =>
        logger.warn(
          s"[WithdrawAdviceRouterConnector] - Exception when withdraw Advice  for eori number $eori, record ID $recordId, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                s"Could not withdraw Advice for eori number $eori, record ID $recordId"
              )
          )
        )
      }
  }

  private def routerWithdrawAdviceUrl(eoriNumber: String, recordId: String, withdrawReason: Option[String]): Url =
    appConfig.routerUrl
      .withPath(UrlPath.parse(s"$routerBaseRoute/traders/$eoriNumber/records/$recordId/advice"))
      .withQueryString(QueryString.fromPairs("withdrawReason" -> withdrawReason))

}

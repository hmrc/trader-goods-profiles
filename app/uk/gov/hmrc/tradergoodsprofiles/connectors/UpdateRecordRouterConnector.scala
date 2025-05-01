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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.JsValue
import play.api.libs.ws.writeableOf_JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.CreateOrUpdateRecordResponse
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpdateRecordRouterConnector @Inject() (
  httpClient: HttpClientV2,
  override val appConfig: AppConfig,
  override val uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with RouterHttpReader
    with Logging {

  def patch(
    eori: String,
    recordId: String,
    updateRecordRequest: Request[JsValue]
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, CreateOrUpdateRecordResponse]] = {
    val url = appConfig.routerUrl.withPath(routerUpdateRecordUrlPath(eori, recordId))
    httpClient
      .patch(url"$url")
      .withContentType
      .withAcceptHeader
      .withBody(updateRecordRequest.body)
      .withClientIdIfSupported //ToDo: Remove this as EIS does not accept the client Id - TGP-1903
      .execute(httpReader[CreateOrUpdateRecordResponse], ec)
      .recover { case ex: Throwable =>
        logAndReturnInternalServerError(eori, recordId, url, ex)
      }
  }

  def put(
    eori: String,
    recordId: String,
    updateRecordRequest: Request[JsValue]
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, CreateOrUpdateRecordResponse]] = {
    val url = appConfig.routerUrl.withPath(routerUpdateRecordUrlPath(eori, recordId))
    httpClient
      .put(url"$url")
      .withContentType
      .withAcceptHeader
      .withBody(updateRecordRequest.body)
      .withClientIdIfSupported
      .execute(httpReader[CreateOrUpdateRecordResponse], ec)
      .recover { case ex: Throwable =>
        logAndReturnInternalServerError(eori, recordId, url, ex)
      }

  }

  private def logAndReturnInternalServerError(
    eori: String,
    recordId: String,
    url: io.lemonlabs.uri.Url,
    ex: Throwable
  ): Left[ServiceError, Nothing] = {
    logger.warn(
      s"""[UpdateRecordRouterConnector] - Exception when updating record for eori number $eori,
               recordId $recordId, url: $url and message ${ex.getMessage}""".stripMargin,
      ex
    )

    Left(
      ServiceError(
        INTERNAL_SERVER_ERROR,
        ErrorResponse
          .serverErrorResponse(uuidService.uuid, "Could not update record due to an internal error")
      )
    )
  }

  private def routerUpdateRecordUrlPath(eori: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eori/records/$recordId"
    )

}

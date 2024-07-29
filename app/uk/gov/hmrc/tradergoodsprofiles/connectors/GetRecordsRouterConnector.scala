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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.models.errors.{ErrorResponse, ServiceError}
import uk.gov.hmrc.tradergoodsprofiles.models.response.{GetRecordResponse, GetRecordsResponse}
import uk.gov.hmrc.tradergoodsprofiles.services.UuidService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRecordsRouterConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  val uuidService: UuidService
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with RouterHttpReader
    with Logging {

  def get(eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ServiceError, GetRecordResponse]] = {
    val url = appConfig.routerUrl.withPath(routerGetRecordUrlPath(eori, recordId))
    httpClient
      .get(url"$url")
      .withClientId
      .withAcceptHeader
      .execute(httpReader[GetRecordResponse], ec)
      .recover { case ex: Throwable =>
        logger.warn(
          s"[GetRecordsRouterConnector] - Exception when retrieving record for eori number $eori and record ID $recordId, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                s"Could not retrieve record for eori number $eori and record ID $recordId"
              )
          )
        )
      }
  }

  def get(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit hc: HeaderCarrier): Future[Either[ServiceError, GetRecordsResponse]] = {
    val url = routerGetRecordsOptionalUrl(eori, lastUpdatedDate, page, size)
    httpClient
      .get(url"$url")
      .withClientId
      .withAcceptHeader
      .execute(httpReader[GetRecordsResponse], ec)
      .recover { case ex: Throwable =>
        logger.warn(
          s"[GetRecordsRouterConnector] - Exception when retrieving multiple records for eori number $eori, with message ${ex.getMessage}",
          ex
        )
        Left(
          ServiceError(
            INTERNAL_SERVER_ERROR,
            ErrorResponse
              .serverErrorResponse(
                uuidService.uuid,
                s"Could not retrieve records for eori number $eori"
              )
          )
        )
      }
  }

  private def routerGetRecordUrlPath(eoriNumber: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eoriNumber/records/$recordId"
    )

  private def routerGetRecordsOptionalUrl(
    eoriNumber: String,
    lastUpdatedDate: Option[String],
    page: Option[Int],
    size: Option[Int]
  ): String = {

    val params = List(
      lastUpdatedDate.map(d => s"lastUpdatedDate=$d"),
      page.map(p => s"page=$p"),
      size.map(s => s"size=$s")
    ).flatten match {
      case Nil   => ""
      case other => other.mkString("?", "&", "")
    }

    val urlPath = appConfig.routerUrl
      .withPath(UrlPath.parse(s"$routerBaseRoute/traders/$eoriNumber/records"))

    s"${urlPath.toString()}$params"
  }
}

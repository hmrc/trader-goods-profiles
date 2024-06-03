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

import com.codahale.metrics.MetricRegistry
import io.lemonlabs.uri.UrlPath
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.metrics.MetricsSupport
import uk.gov.hmrc.tradergoodsprofiles.models.requests.router.{RouterCreateRecordRequest, RouterRequestAccreditationRequest, RouterUpdateRecordRequest}
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.XClientIdHeader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val metricsRegistry: MetricRegistry
)(implicit ec: ExecutionContext)
    extends MetricsSupport
    with Logging {

  def get(eori: String, recordId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.getrecord.connector") { _ =>
      val url = appConfig.routerUrl.withPath(routerRoute(eori, recordId))

      httpClient
        .get(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withClientId
        .execute[HttpResponse]
    }

  def getRecords(
    eori: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  )(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.getrecords.connector") { _ =>
      val url = appConfig.routerUrl.toUrl + routerRouteGetRecords(eori, lastUpdatedDate, page, size)
      httpClient
        .get(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withClientId
        .execute[HttpResponse]
    }

  def createRecord(createRecordRequest: RouterCreateRecordRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.createrecord.connector") { _ =>
      val url      = appConfig.routerUrl.withPath(routerCreateOrUpdateRoute())
      val jsonData = Json.toJson(createRecordRequest)
      httpClient
        .post(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withBody(jsonData)
        .withClientId
        .execute[HttpResponse]
    }

  def removeRecord(eori: String, recordId: String, actorId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.removerecord.connector") { _ =>
      val url = appConfig.routerUrl.withPath(routerRoute(eori, recordId))

      httpClient
        .put(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withClientId
        .withBody(Json.obj("actorId" -> actorId))
        .execute[HttpResponse]
    }

  def updateRecord(updateRecordRequest: RouterUpdateRecordRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.updaterecord.connector") { _ =>
      val url      = appConfig.routerUrl.withPath(routerCreateOrUpdateRoute())
      val jsonData = Json.toJson(updateRecordRequest)
      httpClient
        .put(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withBody(jsonData)
        .withClientId
        .execute[HttpResponse]
    }

  def requestAccreditation(
    accreditationRequest: RouterRequestAccreditationRequest
  )(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.requestaccreditation.connector") { _ =>
      val url      = appConfig.routerUrl.withPath(routerAccreditationRoute())
      val jsonData = Json.toJson(accreditationRequest)
      httpClient
        .post(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withBody(jsonData)
        .withClientId
        .execute[HttpResponse]
    }

  val routerBaseRoute: String = "/trader-goods-profiles-router"

  private def routerRoute(eoriNumber: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/$eoriNumber/records/$recordId"
    )

  private def routerRouteGetRecords(
    eoriNumber: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ): String = {
    val uri = uri"$routerBaseRoute/$eoriNumber?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"
    s"$uri"
  }

  private def routerCreateOrUpdateRoute(): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/records"
    )

  private def routerAccreditationRoute(): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/createaccreditation"
    )

  implicit class HttpResponseHelpers(requestBuilder: RequestBuilder) {
    def withClientId(implicit hc: HeaderCarrier): RequestBuilder =
      hc.headers(Seq(XClientIdHeader)).headOption match {
        case Some(header) => requestBuilder.setHeader(header)
        case None         => requestBuilder
      }
  }
}

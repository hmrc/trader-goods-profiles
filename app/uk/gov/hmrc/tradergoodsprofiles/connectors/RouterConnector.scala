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
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.metrics.MetricsSupport
import uk.gov.hmrc.tradergoodsprofiles.models.{APICreateRecordRequest, RouterCreateRecordRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig,
  override val metricsRegistry: MetricRegistry
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with MetricsSupport
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

  def post(createRecordRequest: RouterCreateRecordRequest)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    withMetricsTimerAsync("tgp.createrecord.connector") { _ =>
      val url      = appConfig.routerUrl.withPath(routerCreateRoute())
      val jsonData = Json.toJson(createRecordRequest)
      httpClient
        .post(url"$url")
        .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
        .withBody(jsonData)
        .withClientId
        .execute[HttpResponse]
    }
}

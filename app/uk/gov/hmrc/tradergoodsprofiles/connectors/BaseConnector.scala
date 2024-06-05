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
import sttp.model.Uri.UriContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.tradergoodsprofiles.config.Constants

trait BaseConnector {

  val routerBaseRoute: String = "/trader-goods-profiles-router"

  def routerRoute(eoriNumber: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/$eoriNumber/records/$recordId"
    )

  def routerRouteGetRecords(
    eoriNumber: String,
    lastUpdatedDate: Option[String] = None,
    page: Option[Int] = None,
    size: Option[Int] = None
  ): String = {
    val uri = uri"$routerBaseRoute/$eoriNumber?lastUpdatedDate=$lastUpdatedDate&page=$page&size=$size"
    s"$uri"
  }

  def routerCreateOrUpdateRoute(): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/records"
    )

  def routerMaintainProfileRoute(): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/profile/maintain"
    )

  implicit class HttpResponseHelpers(requestBuilder: RequestBuilder) {
    def withClientId(implicit hc: HeaderCarrier): RequestBuilder =
      hc.headers(Seq(Constants.XClientIdHeader)).headOption match {
        case Some(header) => requestBuilder.setHeader(header)
        case None         => requestBuilder
      }
  }
}

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

import io.lemonlabs.uri._
import play.api.Logging
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.XClientIdHeader

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RouterConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  val routerBaseRoute: String = "/trader-goods-profiles-router"

  def routerMaintainProfile(eori: String, updateProfileRequest: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url = appConfig.routerUrl.withPath(routerMaintainProfileUrlPath(eori))
    httpClient
      .put(url"$url")
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(updateProfileRequest.body)
      .withClientId
      .execute[HttpResponse]
  }

  def requestAdvice(
    adviceRequest: Request[JsValue],
    eori: String,
    recordId: String
  )(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url      = appConfig.routerUrl.withPath(routerAdviceUrlPath(eori, recordId))
    val jsonData = Json.toJson(adviceRequest.body)
    httpClient
      .post(url"$url")
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(jsonData)
      .withClientId
      .execute[HttpResponse]
  }

  private def routerAdviceUrlPath(eori: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eori/records/$recordId/advice"
    )

  private def routerMaintainProfileUrlPath(eoriNumber: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eoriNumber"
    )

  implicit class HttpResponseHelpers(requestBuilder: RequestBuilder) {
    def withClientId(implicit hc: HeaderCarrier): RequestBuilder =
      hc.headers(Seq(XClientIdHeader)).headOption match {
        case Some(header) => requestBuilder.setHeader(header)
        case None         => requestBuilder
      }
  }
}

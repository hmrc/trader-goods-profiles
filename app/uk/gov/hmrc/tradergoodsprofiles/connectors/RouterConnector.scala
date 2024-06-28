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
import io.lemonlabs.uri.typesafe.QueryKey.stringQueryKey
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

  val routerBaseRoute: String    = "/trader-goods-profiles-router"

  def get(eori: String, recordId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = appConfig.routerUrl.withPath(routerGetRecordUrlPath(eori, recordId))

    httpClient
      .get(url"$url")
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
  ): Future[HttpResponse] = {
    val url = routerGetRecordsOptionalUrl(eori, lastUpdatedDate, page, size)
    httpClient
      .get(url"$url")
      .withClientId
      .execute[HttpResponse]
  }

  def createRecord(eori: String, createRecordRequest: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url = appConfig.routerUrl.withPath(routerCreateRecordUrlPath(eori))

    httpClient
      .post(url"$url")
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(createRecordRequest.body)
      .withClientId
      .execute[HttpResponse]
  }

  def removeRecord(eori: String, recordId: String, actorId: String)(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url = routerRemoveRecordUrl(eori, recordId, actorId)

    httpClient
      .delete(url"$url")
      .withClientId
      .execute[HttpResponse]
  }

  def updateRecord(eori: String, recordId: String, updateRecordRequest: Request[JsValue])(implicit
    hc: HeaderCarrier
  ): Future[HttpResponse] = {
    val url = appConfig.routerUrl.withPath(routerUpdateRecordUrlPath(eori, recordId))
    httpClient
      .patch(url"$url")
      .setHeader(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
      .withBody(updateRecordRequest.body)
      .withClientId
      .execute[HttpResponse]
  }

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

  private def routerGetRecordUrlPath(eoriNumber: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eoriNumber/records/$recordId"
    )

  private def routerRemoveRecordUrl(eoriNumber: String, recordId: String, actorId: String): Url =
    appConfig.routerUrl
      .withPath(UrlPath.parse(s"$routerBaseRoute/traders/$eoriNumber/records/$recordId"))
      .withQueryString(QueryString.fromPairs("actorId" -> actorId))

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

  private def routerUpdateRecordUrlPath(eori: String, recordId: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eori/records/$recordId"
    )

  private def routerCreateRecordUrlPath(eori: String): UrlPath =
    UrlPath.parse(
      s"$routerBaseRoute/traders/$eori/records"
    )

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

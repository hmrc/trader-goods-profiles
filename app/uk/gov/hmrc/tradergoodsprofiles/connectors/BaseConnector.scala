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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.tradergoodsprofiles.config.AppConfig
import uk.gov.hmrc.tradergoodsprofiles.utils.ApplicationConstants.XClientIdHeader

trait BaseConnector {

  val appConfig: AppConfig
  val routerBaseRoute: String = "/trader-goods-profiles-router"

  /* TGP-1889
  ToDo: Remove the  withClientId and withClientIdIfSupported after drop1.1
  as EIS does not accept the client Id i the header anymore.
   */
  implicit class HttpResponseHelpers(requestBuilder: RequestBuilder) {
    def withClientId(implicit hc: HeaderCarrier): RequestBuilder =
      hc.headers(Seq(XClientIdHeader)).headOption match {
        case Some(header) => requestBuilder.setHeader(header)
        case None         => requestBuilder
      }

    def withClientIdIfSupported(implicit hc: HeaderCarrier): RequestBuilder =
      if (appConfig.isDrop1_1_enabled) requestBuilder
      else {
        hc.headers(Seq(XClientIdHeader)).headOption match {
          case Some(header) => requestBuilder.setHeader(header)
          case None         => requestBuilder
        }
      }
  }

}

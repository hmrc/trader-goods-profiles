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

package uk.gov.hmrc.tradergoodsprofiles.utils

object ApplicationConstants {
  val InvalidHeaderParameter          = "INVALID_HEADER_PARAMETER"
  val InvalidRequestParameter         = "INVALID_REQUEST_PARAMETER"
  val InvalidHeaderClientId           = 6000
  val InvalidHeaderClientIdMessage    = "X-Client-ID was missing from Header or is in wrong format"
  val InvalidHeaderContentType        = 3
  val InvalidHeaderContentTypeMessage = "Content-Type was missing from Header or is in the wrong format"
  val InvalidHeaderAccept             = 4
  val InvalidHeaderAcceptMessage      = "Accept was missing from Header or is in wrong format"
  val InvalidActorId                  = 8
  val InvalidActorMessage             = "Mandatory field actorId was missing from body or is in wrong format"
  val InvalidRecordId                 = 25
  val InvalidRecordIdMessage          = "The recordId has been provided in the wrong format"
  val InvalidJson                     = 0
  val InvalidJsonMessage              = "JSON body doesn’t match the schema"

}

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
  val InvalidHeaderParameter                       = "INVALID_HEADER_PARAMETER"
  val InvalidRequestParameter                      = "INVALID_REQUEST_PARAMETER"
  val InvalidHeaderClientId                        = 6000
  val InvalidHeaderClientIdMessage                 = "X-Client-ID was missing from Header or is in wrong format"
  val InvalidHeaderContentType                     = 3
  val InvalidHeaderContentTypeMessage              = "Content-Type was missing from Header or is in the wrong format"
  val InvalidHeaderAccept                          = 4
  val InvalidHeaderAcceptMessage                   = "Accept was missing from Header or is in wrong format"
  val InvalidActorId                               = 8
  val InvalidActorMessage                          = "Mandatory field actorId was missing from body or is in the wrong format"
  val InvalidRecordId                              = 25
  val InvalidRecordIdMessage                       = "The recordId has been provided in the wrong format"
  val InvalidJson                                  = 0
  val InvalidJsonMessage                           = "JSON body doesn’t match the schema"
  val InvalidOrMissingTraderRef                    = "Mandatory field traderRef was missing from body or is in the wrong format"
  val InvalidOrMissingTraderRefCode                = 9
  val InvalidOrMissingComcode                      = "Mandatory field comcode was missing from body or is in the wrong format"
  val InvalidOrMissingComcodeCode                  = 11
  val InvalidOrMissingGoodsDescription             =
    "Mandatory field goodsDescription was missing from body or is in the wrong format"
  val InvalidOrMissingGoodsDescriptionCode         = 12
  val InvalidOrMissingCountryOfOrigin              =
    "Mandatory field countryOfOrigin was missing from body or is in the wrong format"
  val InvalidOrMissingCountryOfOriginCode          = 13
  val InvalidOrMissingCategory                     = "Mandatory field category was missing from body or is in the wrong format"
  val InvalidOrMissingCategoryCode                 = 14
  val InvalidOrMissingComcodeEffectiveFromDate     =
    "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format"
  val InvalidOrMissingComcodeEffectiveFromDateCode = 23

}
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
  val XClientIdHeader                                          = "X-Client-ID"
  val InvalidHeaderParameter                                   = "INVALID_HEADER_PARAMETER"
  val InvalidRequestParameter                                  = "INVALID_REQUEST_PARAMETER"
  val InvalidHeaderClientId                                    = 6000
  val InvalidHeaderClientIdMessage                             = "X-Client-ID was missing from Header or is in wrong format"
  val InvalidHeaderContentType                                 = 3
  val InvalidHeaderContentTypeMessage                          = "Content-Type was missing from Header or is in the wrong format"
  val InvalidHeaderAccept                                      = 4
  val InvalidHeaderAcceptMessage                               = "Accept was missing from Header or is in wrong format"
  val InvalidActorId                                           = 8
  val InvalidActorMessage                                      = "Mandatory field actorId was missing from body or is in the wrong format"
  val InvalidRecordId                                          = 25
  val InvalidRecordIdMessage                                   = "The recordId has been provided in the wrong format"
  val InvalidOrMissingTraderRef                                = "Mandatory field traderRef was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalTraderRef                        = "Optional field traderRef was missing from body or is in the wrong format"
  val InvalidOrMissingTraderRefCode                            = 9
  val InvalidOrMissingComcode                                  = "Mandatory field comcode was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalComcode                          = "Optional field comcode was missing from body or is in the wrong format"
  val InvalidOrMissingComcodeCode                              = 11
  val InvalidOrMissingGoodsDescription                         =
    "Mandatory field goodsDescription was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalGoodsDescription                 =
    "Optional field goodsDescription was missing from body or is in the wrong format"
  val InvalidOrMissingGoodsDescriptionCode                     = 12
  val InvalidOrMissingCountryOfOrigin                          =
    "Mandatory field countryOfOrigin was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalCountryOfOrigin                  =
    "Optional field countryOfOrigin was missing from body or is in the wrong format"
  val InvalidOrMissingCountryOfOriginCode                      = 13
  val InvalidOrMissingCategory                                 = "Mandatory field category was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalCategory                         = "Optional field category was missing from body or is in the wrong format"
  val InvalidOrMissingCategoryCode                             = 14
  val InvalidOrMissingAssessmentIdCode                         = 15
  val InvalidOrMissingAssessmentId                             = "Optional field assessmentId is in the wrong format"
  val InvalidAssessmentPrimaryCategoryCode                     = 16
  val InvalidAssessmentPrimaryCategory                         = "Optional field primaryCategory is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionTypeCode        = 17
  val InvalidAssessmentPrimaryCategoryConditionType            =
    "Optional field type is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionIdCode          = 18
  val InvalidAssessmentPrimaryCategoryConditionId              =
    "Optional field conditionId is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionDescriptionCode = 19
  val InvalidAssessmentPrimaryCategoryConditionDescription     =
    "Optional field conditionDescription is in the wrong format"
  val InvalidAssessmentPrimaryCategoryConditionTraderTextCode  = 20
  val InvalidAssessmentPrimaryCategoryConditionTraderText      =
    "Optional field conditionTraderText is in the wrong format"
  val InvalidOrMissingSupplementaryUnitCode                    = 21
  val InvalidOrMissingMeasurementUnitCode                      = 22
  val InvalidOrMissingMeasurementUnit                          = "Optional field measurementUnit is in the wrong format"
  val InvalidOrMissingComcodeEffectiveFromDateCode             = 23
  val InvalidOrMissingComcodeEffectiveFromDate                 =
    "Mandatory field comcodeEffectiveFromDate was missing from body or is in the wrong format"
  val InvalidOrMissingOptionalComcodeEffectiveFromDate         =
    "Optional field comcodeEffectiveFromDate was missing from body or is in the wrong format"
  val InvalidLastUpdatedDate                                   = "The URL parameter lastUpdatedDate is in the wrong format"
  val InvalidLastUpdatedDateCode                               = 28
  val InvalidOrMissingComcodeEffectiveToDate                   = "Optional field comcodeEffectiveToDate is in the wrong format"
  val InvalidOrMissingComcodeEffectiveToDateCode               = 24
  val InvalidOrMissingSupplementaryUnit                        = "Optional field supplementaryUnit is in the wrong format"
  val InvalidOrMissingUkimsNumberCode                          = 33
  val InvalidOrMissingUkimsNumber                              = "Mandatory field ukimsNumber was missing from body or is in the wrong format"
  val InvalidOrMissingNirmsNumberCode                          = 34
  val InvalidOrMissingNirmsNumber                              = "Optional field nirmsNumber is in the wrong format"
  val InvalidOrMissingNiphlNumberCode                          = 35
  val InvalidOrMissingNiphlNumber                              = "Optional field niphlNumber is in the wrong format"
  val InvalidOrMissingRequestorNameCode                        = 1008
  val InvalidOrMissingRequestorName                            = "Mandatory field requestorName was missing from body or is in the wrong format"
  val InvalidOrMissingRequestorEmailCode                       = 1009
  val InvalidOrMissingRequestorEmail                           = "Mandatory field requestorEmail was missing from body or is in the wrong format"
}

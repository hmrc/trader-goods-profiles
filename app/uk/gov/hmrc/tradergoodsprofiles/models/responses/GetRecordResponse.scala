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

package uk.gov.hmrc.tradergoodsprofiles.models.response

import play.api.libs.json.*
import uk.gov.hmrc.tradergoodsprofiles.models.Assessment
import uk.gov.hmrc.tradergoodsprofiles.utils.ResponseModelSupport.{convertReviewReason, removeNulls}

import java.time.Instant

case class GetRecordResponse(
  eori: String,
  actorId: String,
  recordId: String,
  traderRef: String,
  comcode: String,
  adviceStatus: String,
  goodsDescription: String,
  countryOfOrigin: String,
  category: Option[Int],
  assessments: Option[Seq[Assessment]],
  supplementaryUnit: Option[BigDecimal],
  measurementUnit: Option[String],
  comcodeEffectiveFromDate: Instant,
  comcodeEffectiveToDate: Option[Instant],
  version: Int,
  active: Boolean,
  toReview: Boolean,
  reviewReason: Option[String],
  declarable: String,
  ukimsNumber: String,
  nirmsNumber: Option[String],
  niphlNumber: Option[String],
  locked: Boolean,
  createdDateTime: Instant,
  updatedDateTime: Instant
)

object GetRecordResponse {
  implicit lazy val format: OFormat[GetRecordResponse] = new OFormat[GetRecordResponse] {

    override def writes(o: GetRecordResponse): JsObject = {

      val fields = Seq(
        "eori"                     -> Json.toJson(o.eori),
        "actorId"                  -> Json.toJson(o.actorId),
        "recordId"                 -> Json.toJson(o.recordId),
        "traderRef"                -> Json.toJson(o.traderRef),
        "comcode"                  -> Json.toJson(o.comcode),
        "adviceStatus"             -> Json.toJson(o.adviceStatus),
        "goodsDescription"         -> Json.toJson(o.goodsDescription),
        "countryOfOrigin"          -> Json.toJson(o.countryOfOrigin),
        "category"                 -> Json.toJson(o.category),
        "assessments"              -> Json.toJson(o.assessments),
        "supplementaryUnit"        -> Json.toJson(o.supplementaryUnit),
        "measurementUnit"          -> Json.toJson(o.measurementUnit),
        "comcodeEffectiveFromDate" -> Json.toJson(o.comcodeEffectiveFromDate),
        "comcodeEffectiveToDate"   -> Json.toJson(o.comcodeEffectiveToDate),
        "version"                  -> Json.toJson(o.version),
        "active"                   -> Json.toJson(o.active),
        "toReview"                 -> Json.toJson(o.toReview),
        "reviewReason"             -> Json.toJson(convertReviewReason(o.reviewReason, o.toReview)),
        "declarable"               -> Json.toJson(o.declarable),
        "ukimsNumber"              -> Json.toJson(o.ukimsNumber),
        "nirmsNumber"              -> Json.toJson(o.nirmsNumber),
        "niphlNumber"              -> Json.toJson(o.niphlNumber),
        "locked"                   -> Json.toJson(o.locked),
        "createdDateTime"          -> Json.toJson(o.createdDateTime),
        "updatedDateTime"          -> Json.toJson(o.updatedDateTime)
      )
      removeNulls(JsObject(fields))
    }

    override def reads(json: JsValue): JsResult[GetRecordResponse] = {

      def read[A: Reads](name: String): JsResult[A] = {
        val bpath    = json \ name
        val path     = JsPath() \ name
        val resolved = path.asSingleJsResult(json)
        val result   = bpath.validate[A].repath(path)
        if (result.isSuccess) {
          result
        } else {
          resolved.flatMap(_ => result)
        }
      }

      def readOption[A: Reads](name: String): JsResult[Option[A]] = {
        val bpath    = json \ name
        val path     = JsPath() \ name
        val resolved = path.asSingleJsResult(json)
        val result   = bpath.validateOpt[A].repath(path)
        if (result.isSuccess) {
          result
        } else {
          resolved.flatMap(_ => result)
        }
      }

      val recordId                 = read[String]("recordId")
      val eori                     = read[String]("eori")
      val actorId                  = read[String]("actorId")
      val traderRef                = read[String]("traderRef")
      val comcode                  = read[String]("comcode")
      val adviceStatus             = read[String]("adviceStatus")
      val goodsDescription         = read[String]("goodsDescription")
      val countryOfOrigin          = read[String]("countryOfOrigin")
      val category                 = readOption[Int]("category")
      val assessments              = readOption[Seq[Assessment]]("assessments")
      val supplementaryUnit        = readOption[BigDecimal]("supplementaryUnit")
      val measurementUnit          = readOption[String]("measurementUnit")
      val comcodeEffectiveFromDate = read[Instant]("comcodeEffectiveFromDate")
      val comcodeEffectiveToDate   = readOption[Instant]("comcodeEffectiveToDate")
      val version                  = read[Int]("version")
      val active                   = read[Boolean]("active")
      val toReview                 = read[Boolean]("toReview")
      val reviewReason             = readOption[String]("reviewReason")
      val declarable               = read[String]("declarable")
      val ukimsNumber              = read[String]("ukimsNumber")
      val nirmsNumber              = readOption[String]("nirmsNumber")
      val niphlNumber              = readOption[String]("niphlNumber")
      val locked                   = read[Boolean]("locked")
      val createdDateTime          = read[Instant]("createdDateTime")
      val updatedDateTime          = read[Instant]("updatedDateTime")

      val errors = Seq[JsResult[_]](
        eori,
        actorId,
        recordId,
        traderRef,
        comcode,
        adviceStatus,
        goodsDescription,
        countryOfOrigin,
        category,
        assessments,
        supplementaryUnit,
        measurementUnit,
        comcodeEffectiveFromDate,
        comcodeEffectiveToDate,
        version,
        active,
        toReview,
        reviewReason,
        declarable,
        ukimsNumber,
        nirmsNumber,
        niphlNumber,
        locked,
        createdDateTime,
        updatedDateTime
      ).collect { case JsError(values) =>
        values
      }.flatten

      if (errors.isEmpty) {
        try JsSuccess(
          new GetRecordResponse(
            eori.get,
            actorId.get,
            recordId.get,
            traderRef.get,
            comcode.get,
            adviceStatus.get,
            goodsDescription.get,
            countryOfOrigin.get,
            category.get,
            assessments.get,
            supplementaryUnit.get,
            measurementUnit.get,
            comcodeEffectiveFromDate.get,
            comcodeEffectiveToDate.get,
            version.get,
            active.get,
            toReview.get,
            reviewReason.get,
            declarable.get,
            ukimsNumber.get,
            nirmsNumber.get,
            niphlNumber.get,
            locked.get,
            createdDateTime.get,
            updatedDateTime.get
          )
        )
        catch {
          case e: IllegalArgumentException =>
            val sw = new _root_.java.io.StringWriter()
            val pw = new _root_.java.io.PrintWriter(sw)
            e.printStackTrace(pw)
            JsError(JsonValidationError(sw.toString, e))
        }
      } else {
        JsError(errors)
      }
    }
  }

}

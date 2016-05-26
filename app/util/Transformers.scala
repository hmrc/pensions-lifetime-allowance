/*
 * Copyright 2016 HM Revenue & Customs
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

package util

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Transformers {
  private val protectionTypes = Vector(
    "Unknown", "FP2016", "IP2014", "IP2016", "Primary", "Enhanced", "Fixed", " FP2014"
  )

  private val protectionStatuses = Vector(
    "Unknown", "Open", "Dormant", "Withdrawn", "Expired", "Unsuccessful", "Rejected"
  )

  private def rename(origName: String, newName: String): Reads[JsObject] =
    (__ \ newName).json.copyFrom((__ \ origName).json.pick)

  private def renameIfExists(origName: String, newName: String): Reads[JsObject] =
    rename(origName, newName) orElse Reads.pure(Json.obj())

  private def string2Int(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    (__ \ fieldName).json.update(of[JsString].map(s => JsNumber(lookupTable.indexOf(s.value))))

  private def int2String(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    (__ \ fieldName).json.update(of[JsNumber].map(n => JsString(lookupTable(n.value.toInt))))

  private def string2IntIfExists(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    string2Int(fieldName, lookupTable) orElse Reads.pure(Json.obj())

  private def int2StringIfExists(fieldName: String, lookupTable: Seq[String]): Reads[JsObject] =
    int2String(fieldName, lookupTable) orElse Reads.pure(Json.obj())

  /**
    * Transform an incoming MDTP API protection application request body Json to a request body for the corresponding
    * outbound DES API Create Lifetime Allowance request
    * @param ninoWithoutSuffix the NINO with the suffix character dropped, as per DES API requirements
    * @param mdtpApplicationJson the incoming protecion applicaion request body
    * @return
    */
  def mdtpApplicationToDesCreatePLARequestBody(ninoWithoutSuffix: String, mdtpApplicationJson: JsObject): JsResult[JsObject] = {
    val desProtectionFromMdtpApplication =
      (rename("protectionType", "type") andThen string2Int("type", protectionTypes)) and
        renameIfExists("postADayBenefitCrystallisationEvents", "postADayBCE") reduce
    val desRequestBodyFromDesProtection = __.json.pickBranch(
      (__ \ 'nino).json.put(JsString(ninoWithoutSuffix)) and
        (__ \ 'protection).json.copyFrom((__).json.pick) reduce
    )
    for (
      desProtectionJson <- mdtpApplicationJson.transform(desProtectionFromMdtpApplication);
      desRequestBody <- desProtectionJson.transform(desRequestBodyFromDesProtection)
    ) yield desRequestBody
  }

  /**
    * Transform a received DES Create API response body into the MDTP API equivalent to be returned to the client of this
    * service.
    * @param ninoSuffix the last character of the NINO associated with the request - needs to be appended to the
    *                   NINO returned by the DES API
    * @param desResponseJson the json body received from DES in response to a Create Lifetime Allowance request.
    * @return a Json body for return to the MDTP service client.
    */

  def desCreatePLAResponseBodyToMdtpProtection(ninoSuffix: Char, desResponseJson: JsObject): JsResult[JsObject] = {

    def copyToTopLevel(fieldName: String): Reads[JsObject] =
      (__ \ fieldName).json.copyFrom((__ \ "protection" \ fieldName).json.pick)
    def copyToTopLevelIfExists(fieldName: String) = copyToTopLevel(fieldName) orElse Reads.pure(Json.obj())

    def copyProtectionDetailsToTopLevel: Reads[JsObject] =
      ( copyToTopLevelIfExists("id") and
        copyToTopLevelIfExists("version") and
        (copyToTopLevel("type") andThen rename("type", "protectionType") andThen int2String("protectionType", protectionTypes)) and
        (copyToTopLevelIfExists("status") andThen int2StringIfExists("status", protectionStatuses)) and
        copyToTopLevelIfExists("relevantAmount") and
        copyToTopLevelIfExists("preADayPensionInPayment") and
        copyToTopLevelIfExists("uncrystallisedRights") and
        copyToTopLevelIfExists("nonUKRights") and
        copyToTopLevelIfExists("notificationID") and
        copyToTopLevelIfExists("protectionReference")  reduce)

    def readCertificateDateOpt = (__ \ "protection" \ "certificateDate").readNullable[String]
    def readCertificateTimeOpt = (__ \ "protection" \ "certificateTime").readNullable[String]

    // DES returns date and time in separate fields, but MDTP API requires a single ISOO601 date/time field.
    // soo need to merge the DES data and time fields to create the full field for the MDTP API.
    def iso8601CertDateOpt: Option[String] = {
      val desCertDateJs = desResponseJson.validate[Option[String]](readCertificateDateOpt)
      val desCertTimeJs = desResponseJson.validate[Option[String]](readCertificateTimeOpt)
      (desCertDateJs, desCertTimeJs) match {
        case (d: JsSuccess[Option[String]], t: JsSuccess[Option[String]]) if d.value.isDefined && t.value.isDefined =>
          Some(d.value.get + "T" + t.value.get)
        case (d: JsSuccess[Option[String]], _) if d.value.isDefined => Some(d.value.get)
        case _ => None
      }
    }

    val certificateDateOpt=iso8601CertDateOpt

    val toMdtpProtection =
      ((__ \ 'nino).json.update(of[JsString].map { case JsString(s) => JsString(s + ninoSuffix) }) and
       copyProtectionDetailsToTopLevel and
      (
        // replace certificate date with full ISO8601 date/time
        copyToTopLevelIfExists("certificateDate")
          andThen (__ \ "certificateDate").json.update(of[JsString].map { s => JsString(iso8601CertDateOpt.get) })
          orElse Reads.pure(Json.obj())
      ) reduce) andThen (__ \ "protection").json.prune

    desResponseJson.transform(toMdtpProtection)
  }
}
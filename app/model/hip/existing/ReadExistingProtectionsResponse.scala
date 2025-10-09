/*
 * Copyright 2025 HM Revenue & Customs
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

package model.hip.existing

import play.api.libs.json.{Format, Json}

case class ReadExistingProtectionsResponse(
    pensionSchemeAdministratorCheckReference: String,
    protectionRecordsList: Option[Seq[ProtectionRecordsList]]
) {

  def padCertificateTime: ReadExistingProtectionsResponse =
    copy(protectionRecordsList = protectionRecordsList.map(_.map(_.padCertificateTime)))

}

object ReadExistingProtectionsResponse {
  implicit val format: Format[ReadExistingProtectionsResponse] = Json.format[ReadExistingProtectionsResponse]
}

case class ProtectionRecordsList(
    protectionRecord: ProtectionRecord,
    historicaldetailsList: Option[Seq[ProtectionRecord]]
) {

  def padCertificateTime: ProtectionRecordsList = copy(
    protectionRecord = protectionRecord.padCertificateTime,
    historicaldetailsList = historicaldetailsList.map(_.map(_.padCertificateTime))
  )

}

object ProtectionRecordsList {
  implicit val format: Format[ProtectionRecordsList] = Json.format[ProtectionRecordsList]
}

case class ProtectionRecord(
    identifier: Long,
    sequenceNumber: Int,
    `type`: ProtectionType,
    certificateDate: String,
    certificateTime: String,
    status: ProtectionStatus,
    protectionReference: Option[String],
    relevantAmount: Option[Int],
    preADayPensionInPaymentAmount: Option[Int],
    postADayBenefitCrystallisationEventAmount: Option[Int],
    uncrystallisedRightsAmount: Option[Int],
    nonUKRightsAmount: Option[Int],
    pensionDebitAmount: Option[Int],
    pensionDebitEnteredAmount: Option[Int],
    protectedAmount: Option[Int],
    pensionDebitStartDate: Option[String],
    pensionDebitTotalAmount: Option[Int],
    lumpSumAmount: Option[Int],
    lumpSumPercentage: Option[Int],
    enhancementFactor: Option[Double]
) {

  def padCertificateTime: ProtectionRecord = copy(
    certificateTime = ProtectionRecord.padCertificateTime(certificateTime)
  )

}

object ProtectionRecord {
  implicit val format: Format[ProtectionRecord] = Json.format[ProtectionRecord]

  private val MIN_CERTIFICATE_TIME_LENGTH = 6

  private[hip] def padCertificateTime(certificateTimeString: String): String = {
    val padding = "0" * (MIN_CERTIFICATE_TIME_LENGTH - certificateTimeString.length).max(0)

    s"$padding$certificateTimeString"
  }

}

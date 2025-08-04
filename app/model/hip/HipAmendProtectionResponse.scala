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

package model.hip

import model.api.AmendProtectionResponse
import play.api.libs.json.{Format, Json}

case class HipAmendProtectionResponse(
    updatedLifetimeAllowanceProtectionRecord: UpdatedLifetimeAllowanceProtectionRecord
) {

  def toAmendProtectionResponse: AmendProtectionResponse = AmendProtectionResponse(
    lifetimeAllowanceIdentifier = updatedLifetimeAllowanceProtectionRecord.identifier,
    lifetimeAllowanceSequenceNumber = updatedLifetimeAllowanceProtectionRecord.sequenceNumber,
    lifetimeAllowanceType = updatedLifetimeAllowanceProtectionRecord.`type`,
    certificateDate = updatedLifetimeAllowanceProtectionRecord.certificateDate,
    certificateTime = updatedLifetimeAllowanceProtectionRecord.certificateTime,
    status = updatedLifetimeAllowanceProtectionRecord.status,
    protectionReference = updatedLifetimeAllowanceProtectionRecord.protectionReference,
    relevantAmount = updatedLifetimeAllowanceProtectionRecord.relevantAmount,
    preADayPensionInPaymentAmount = updatedLifetimeAllowanceProtectionRecord.preADayPensionInPaymentAmount,
    postADayBenefitCrystallisationEventAmount =
      updatedLifetimeAllowanceProtectionRecord.postADayBenefitCrystallisationEventAmount,
    uncrystallisedRightsAmount = updatedLifetimeAllowanceProtectionRecord.uncrystallisedRightsAmount,
    nonUKRightsAmount = updatedLifetimeAllowanceProtectionRecord.nonUKRightsAmount,
    pensionDebitAmount = updatedLifetimeAllowanceProtectionRecord.pensionDebitAmount,
    pensionDebitEnteredAmount = updatedLifetimeAllowanceProtectionRecord.pensionDebitEnteredAmount,
    notificationIdentifier = updatedLifetimeAllowanceProtectionRecord.notificationIdentifier,
    protectedAmount = updatedLifetimeAllowanceProtectionRecord.protectedAmount,
    pensionDebitStartDate = updatedLifetimeAllowanceProtectionRecord.pensionDebitStartDate,
    pensionDebitTotalAmount = updatedLifetimeAllowanceProtectionRecord.pensionDebitTotalAmount
  )

}

object HipAmendProtectionResponse {
  implicit val format: Format[HipAmendProtectionResponse] = Json.format[HipAmendProtectionResponse]
}

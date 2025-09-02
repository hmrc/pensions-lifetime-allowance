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

package testdata

import model.api.{AmendProtectionRequest, AmendProtectionResponse}
import model.hip._
import model.hip.existing.ProtectionStatus.{Open, Withdrawn}
import model.hip.existing.ProtectionType.{FixedProtection2016LTA, PensionCreditRights}
import model.hip.existing.{ProtectionRecord, ProtectionRecordsList, ReadExistingProtectionsResponse}

object HipTestData {

  val lifetimeAllowanceIdentifier: Int     = 42
  val lifetimeAllowanceSequenceNumber: Int = 13
  val protectionReference: String          = "IP141034571625B"

  val amendProtectionRequest: AmendProtectionRequest = AmendProtectionRequest(
    lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
    lifetimeAllowanceType = AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta,
    certificateDate = Some("2025-07-15"),
    certificateTime = Some("174312"),
    status = AmendProtectionRequestStatus.Open,
    protectionReference = Some(protectionReference),
    relevantAmount = 105000,
    preADayPensionInPaymentAmount = 1500,
    postADayBenefitCrystallisationEventAmount = 2500,
    uncrystallisedRightsAmount = 75500,
    nonUKRightsAmount = 0,
    pensionDebitAmount = Some(25000),
    pensionDebitEnteredAmount = Some(25000),
    notificationIdentifier = Some(3),
    protectedAmount = Some(120000),
    pensionDebitStartDate = Some("2026-07-09"),
    pensionDebitTotalAmount = Some(40000)
  )

  val amendProtectionResponse: AmendProtectionResponse = AmendProtectionResponse(
    lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
    lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber + 1,
    lifetimeAllowanceType = AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta,
    certificateDate = Some("2025-07-15"),
    certificateTime = Some("174312"),
    status = AmendProtectionResponseStatus.Open,
    protectionReference = Some(protectionReference),
    relevantAmount = 105000,
    preADayPensionInPaymentAmount = 1500,
    postADayBenefitCrystallisationEventAmount = 2500,
    uncrystallisedRightsAmount = 75500,
    nonUKRightsAmount = 0,
    pensionDebitAmount = Some(25000),
    pensionDebitEnteredAmount = Some(25000),
    notificationIdentifier = Some(3),
    protectedAmount = Some(120000),
    pensionDebitStartDate = Some("2026-07-09"),
    pensionDebitTotalAmount = Some(40000)
  )

  val hipAmendProtectionRequest: HipAmendProtectionRequest = HipAmendProtectionRequest(
    LifetimeAllowanceProtectionRecord(
      `type` = AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta,
      certificateDate = Some("2025-07-15"),
      certificateTime = Some("174312"),
      status = AmendProtectionRequestStatus.Open,
      protectionReference = Some(protectionReference),
      relevantAmount = 105000,
      preADayPensionInPaymentAmount = 1500,
      postADayBenefitCrystallisationEventAmount = 2500,
      uncrystallisedRightsAmount = 75500,
      nonUKRightsAmount = 0,
      pensionDebitAmount = Some(25000),
      pensionDebitEnteredAmount = Some(25000),
      notificationIdentifier = Some(3),
      protectedAmount = Some(120000),
      pensionDebitStartDate = Some("2026-07-09"),
      pensionDebitTotalAmount = Some(40000)
    )
  )

  val hipAmendProtectionResponse: HipAmendProtectionResponse = HipAmendProtectionResponse(
    UpdatedLifetimeAllowanceProtectionRecord(
      identifier = lifetimeAllowanceIdentifier,
      sequenceNumber = lifetimeAllowanceSequenceNumber + 1,
      `type` = AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta,
      certificateDate = Some("2025-07-15"),
      certificateTime = Some("174312"),
      status = AmendProtectionResponseStatus.Open,
      protectionReference = Some(protectionReference),
      relevantAmount = 105000,
      preADayPensionInPaymentAmount = 1500,
      postADayBenefitCrystallisationEventAmount = 2500,
      uncrystallisedRightsAmount = 75500,
      nonUKRightsAmount = 0,
      pensionDebitAmount = Some(25000),
      pensionDebitEnteredAmount = Some(25000),
      notificationIdentifier = Some(3),
      protectedAmount = Some(120000),
      pensionDebitStartDate = Some("2026-07-09"),
      pensionDebitTotalAmount = Some(40000)
    )
  )

  val hipReadExistingProtectionsResponse: ReadExistingProtectionsResponse = ReadExistingProtectionsResponse(
    pensionSchemeAdministratorCheckReference = "PSA34728911G",
    protectionRecordsList = Seq(
      ProtectionRecordsList(
        ProtectionRecord(
          identifier = 20,
          sequenceNumber = 3,
          `type` = FixedProtection2016LTA,
          certificateDate = "2021-02-19",
          certificateTime = "091732",
          status = Open,
          protectionReference = Some("EPRO1034571625B"),
          lumpSumPercentage = Some(12),
          relevantAmount = Some(5),
          preADayPensionInPaymentAmount = Some(2),
          postADayBenefitCrystallisationEventAmount = Some(6),
          uncrystallisedRightsAmount = Some(8),
          nonUKRightsAmount = Some(10),
          pensionDebitAmount = Some(14),
          pensionDebitEnteredAmount = Some(7),
          protectedAmount = Some(750000),
          pensionDebitStartDate = Some("2019-01-02"),
          pensionDebitTotalAmount = Some(1000000),
          lumpSumAmount = Some(25000),
          enhancementFactor = Some(14.78)
        ),
        Some(
          Seq(
            ProtectionRecord(
              identifier = 12,
              sequenceNumber = 2,
              `type` = PensionCreditRights,
              certificateDate = "2014-06-05",
              certificateTime = "046356",
              status = Withdrawn,
              protectionReference = Some("PCR36774256137A"),
              lumpSumPercentage = Some(11),
              relevantAmount = None,
              preADayPensionInPaymentAmount = None,
              postADayBenefitCrystallisationEventAmount = None,
              uncrystallisedRightsAmount = None,
              nonUKRightsAmount = None,
              pensionDebitAmount = None,
              pensionDebitEnteredAmount = None,
              protectedAmount = None,
              pensionDebitStartDate = None,
              pensionDebitTotalAmount = None,
              lumpSumAmount = None,
              enhancementFactor = None
            )
          )
        )
      )
    )
  )

}

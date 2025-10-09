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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HipAmendProtectionResponseSpec extends AnyWordSpec with Matchers {

  "HipAmendProtectionResponse on padCertificateTime" should {
    "pad certificate time with leading zeros up to a length of 6" in {
      val updatedLifetimeAllowanceProtectionRecord =
        UpdatedLifetimeAllowanceProtectionRecord(
          identifier = 1,
          sequenceNumber = 1,
          `type` = AmendProtectionLifetimeAllowanceType.IndividualProtection2014,
          certificateDate = Some("2025-10-09"),
          certificateTime = Some("93010"),
          status = AmendProtectionResponseStatus.Open,
          protectionReference = Some("IP14000001"),
          relevantAmount = 0,
          preADayPensionInPaymentAmount = 0,
          postADayBenefitCrystallisationEventAmount = 0,
          uncrystallisedRightsAmount = 0,
          nonUKRightsAmount = 0,
          pensionDebitAmount = Some(0),
          pensionDebitEnteredAmount = Some(0),
          notificationIdentifier = Some(1),
          protectedAmount = Some(0),
          pensionDebitStartDate = None,
          pensionDebitTotalAmount = None
        )

      val hipAmendProtectionResponse = HipAmendProtectionResponse(
        updatedLifetimeAllowanceProtectionRecord
      )

      val paddedHipAmendProtectionResponse = HipAmendProtectionResponse(
        updatedLifetimeAllowanceProtectionRecord.copy(
          certificateTime = Some("093010")
        )
      )

      hipAmendProtectionResponse.padCertificateTime shouldBe paddedHipAmendProtectionResponse
    }
  }

}

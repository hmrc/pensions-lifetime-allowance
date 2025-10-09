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

package model.hip.existing

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReadExistingProtectionsResponseSpec extends AnyWordSpec with Matchers {

  val pensionSchemaAdministratorCheckReference = "PSA00001"

  "ReadExistingProtectionsResponseSpec on padCertificateTime" should {
    "pad certificate time with leading zeros up to a length of 6" in {
      val protectionRecord = ProtectionRecord(
        identifier = 1,
        sequenceNumber = 1,
        `type` = ProtectionType.IndividualProtection2014,
        certificateDate = "2025-10-09",
        certificateTime = "93010",
        status = ProtectionStatus.Open,
        protectionReference = Some("IP14000001"),
        relevantAmount = Some(0),
        preADayPensionInPaymentAmount = Some(0),
        postADayBenefitCrystallisationEventAmount = Some(0),
        uncrystallisedRightsAmount = Some(0),
        nonUKRightsAmount = Some(0),
        pensionDebitAmount = Some(0),
        pensionDebitEnteredAmount = Some(0),
        protectedAmount = Some(0),
        pensionDebitStartDate = None,
        pensionDebitTotalAmount = None,
        lumpSumAmount = None,
        lumpSumPercentage = None,
        enhancementFactor = None
      )

      val readExistingProtectionsResponse = ReadExistingProtectionsResponse(
        pensionSchemaAdministratorCheckReference,
        Some(
          Seq(
            ProtectionRecordsList(
              protectionRecord,
              None
            )
          )
        )
      )

      val paddedReadExistingProtectionsResponse = ReadExistingProtectionsResponse(
        pensionSchemaAdministratorCheckReference,
        Some(
          Seq(
            ProtectionRecordsList(
              protectionRecord.copy(
                certificateTime = "093010"
              ),
              None
            )
          )
        )
      )

      readExistingProtectionsResponse.padCertificateTime shouldBe paddedReadExistingProtectionsResponse
    }

  }

  "ProtectionRecord on padCertificateTime" should {
    "pad with leading zeros up to a length of 6" when {
      "certificateTime is 1" in {
        ProtectionRecord.padCertificateTime("1") shouldBe "000001"
      }

      "certificateTime is 11" in {
        ProtectionRecord.padCertificateTime("11") shouldBe "000011"
      }

      "certificateTime is 111" in {
        ProtectionRecord.padCertificateTime("111") shouldBe "000111"
      }

      "certificateTime is 1111" in {
        ProtectionRecord.padCertificateTime("1111") shouldBe "001111"
      }

      "certificateTime is 11111" in {
        ProtectionRecord.padCertificateTime("11111") shouldBe "011111"
      }
    }
  }

}

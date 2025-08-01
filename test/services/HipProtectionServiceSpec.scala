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

package services

import connectors.HipConnector
import model.hip.ReadExistingProtectionsResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import testdata.HipTestData._
import uk.gov.hmrc.http.HeaderCarrier
import util.TestUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HipProtectionServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  private val hipConnector = mock[HipConnector]

  private val hipProtectionService = new HipProtectionService(hipConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val testNino: String = TestUtils.randomNino

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(hipConnector)
  }

  "HipProtectionService on amendProtection" should {

    "call HipConnector providing converted AmendProtectionResponse" in {
      when(hipConnector.amendProtection(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(hipAmendProtectionResponse)))

      hipProtectionService.amendProtection(testNino, lifetimeAllowanceIdentifier, amendProtectionRequest).futureValue

      verify(hipConnector).amendProtection(
        eqTo(testNino),
        eqTo(lifetimeAllowanceIdentifier),
        eqTo(lifetimeAllowanceSequenceNumber),
        eqTo(hipAmendProtectionRequest)
      )(eqTo(hc))
    }

    "return converted AmendProtectionResponse from HipConnector" in {
      when(hipConnector.amendProtection(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(hipAmendProtectionResponse)))

      hipProtectionService
        .amendProtection(testNino, lifetimeAllowanceIdentifier, amendProtectionRequest)
        .futureValue shouldBe Right(amendProtectionResponse)
    }
  }

  "HipProtectionService on readExistingProtections" should {

    "call HipConnector" in {
      when(hipConnector.readExistingProtections())
        .thenReturn(Future.successful(ReadExistingProtectionsResponse("PSA12345678A")))

      hipProtectionService.readExistingProtections().futureValue

      verify(hipConnector).readExistingProtections()
    }

    "return ReadExistingProtectionsResponse from HipConnector" in {
      when(hipConnector.readExistingProtections())
        .thenReturn(Future.successful(ReadExistingProtectionsResponse("PSA12345678A")))

      val result = hipProtectionService.readExistingProtections().futureValue

      result shouldBe ReadExistingProtectionsResponse("PSA12345678A")
    }
  }

}

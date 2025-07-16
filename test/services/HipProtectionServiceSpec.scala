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
import model.hip.{AmendProtectionResponse, ReadExistingProtectionsResponse, UpdatedLifetimeAllowanceProtectionRecord}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class HipProtectionServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  private val hipConnector = mock[HipConnector]

  private val hipProtectionService = new HipProtectionService(hipConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(hipConnector)
  }

  "HipProtectionService on amendProtection" should {

    "call HipConnector" in {
      when(hipConnector.amendProtection())
        .thenReturn(Future.successful(AmendProtectionResponse(UpdatedLifetimeAllowanceProtectionRecord(42))))

      hipProtectionService.amendProtection().futureValue

      verify(hipConnector).amendProtection()
    }

    "return AmendProtectionResponse from HipConnector" in {
      when(hipConnector.amendProtection())
        .thenReturn(Future.successful(AmendProtectionResponse(UpdatedLifetimeAllowanceProtectionRecord(42))))

      val result = hipProtectionService.amendProtection().futureValue

      result shouldBe AmendProtectionResponse(UpdatedLifetimeAllowanceProtectionRecord(42))
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

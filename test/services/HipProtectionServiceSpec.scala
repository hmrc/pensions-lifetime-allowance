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
import model.hip.{AmendProtectionResponse, UpdatedLifetimeAllowanceProtectionRecord}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import util.TestObjects.readExistingProtectionsResponse

import java.util.Random
import scala.concurrent.Future

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

    val rand          = new Random()
    val ninoGenerator = new Generator(rand)

    val nino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

    "return ReadExistingProtectionsResponse from HipConnector" when {

      "it receives 200 response" in {
        when(hipConnector.readExistingProtections(eqTo(nino))(any()))
          .thenReturn(Future.successful(Right(readExistingProtectionsResponse)))

        val result = hipProtectionService.readExistingProtections(nino).futureValue

        result shouldBe Right(readExistingProtectionsResponse)

        verify(hipConnector).readExistingProtections(eqTo(nino))(any())
      }
    }

    "return UpstreamErrorResponse from HipConnector" when
      Seq(BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { statusCode =>
        s"the connector receives a $statusCode response" in {

          val responseBody =
            """
              |{
              |  "origin": "HIP",
              |  "response": {
              |    "failures": [
              |      {
              |        "type": "string",
              |        "reason": "string"
              |      }
              |    ]
              |  }
              |}
              |""".stripMargin

          val response = Left(UpstreamErrorResponse(responseBody, statusCode))

          when(hipConnector.readExistingProtections(eqTo(nino))(any()))
            .thenReturn(Future.successful(response))

          val result = hipProtectionService.readExistingProtections(nino).futureValue

          result shouldBe response

          verify(hipConnector).readExistingProtections(eqTo(nino))(any())
        }
      }
  }

}

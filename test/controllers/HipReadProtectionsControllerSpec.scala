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

package controllers

import connectors.{CitizenDetailsConnector, CitizenRecordOK}
import mock.AuthMock
import model.hip.ReadExistingProtectionsResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import services.HipProtectionService
import uk.gov.hmrc.domain.Generator

import java.util.Random
import scala.concurrent.{ExecutionContext, Future}

class HipReadProtectionsControllerSpec
  extends AnyWordSpec
    with GuiceOneServerPerSuite
    with Matchers
    with ScalaFutures
    with BeforeAndAfterEach
    with AuthMock {


  private implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val citizenDetailsConnector = mock[CitizenDetailsConnector]
  private val hipProtectionService    = mock[HipProtectionService]
  private val cc                      = app.injector.instanceOf[ControllerComponents]

  private val controller = new HipReadProtectionsController(
    mockAuthConnector,
    citizenDetailsConnector,
    hipProtectionService,
    cc
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(citizenDetailsConnector, hipProtectionService)
    mockAuthConnector(Future.successful {})
    when(citizenDetailsConnector.checkCitizenRecord(any[String])(any(), any()))
      .thenReturn(Future.successful(CitizenRecordOK))
  }

  private val rand          = new Random()
  private val ninoGenerator = new Generator(rand)

  private val testNino = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  private val readExistingProtections = ReadExistingProtectionsResponse("PSA12345678A")

  "HipReadProtectionsController on readExistingProtections" should {

    "call HipProtectionService" in {
      when(hipProtectionService.readExistingProtections()(any()))
        .thenReturn(Future.successful(readExistingProtections))

      val request = FakeRequest(method = "POST", path = "/")

      controller.readExistingProtections(testNino)(request).futureValue

      verify(hipProtectionService).readExistingProtections()(any())
    }

    "return the value obtained from HipProtectionService" in {
      when(hipProtectionService.readExistingProtections()(any()))
        .thenReturn(Future.successful(readExistingProtections))

      val request = FakeRequest(method = "POST", path = "/")

      val result = controller.readExistingProtections(testNino)(request)


      status(result) shouldBe OK
      contentAsJson(result).as[ReadExistingProtectionsResponse] shouldBe readExistingProtections
    }
  }

}

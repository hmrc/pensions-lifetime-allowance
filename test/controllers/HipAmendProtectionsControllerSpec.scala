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
import model.api.AmendProtectionResponse
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeHeaders, FakeRequest}
import services.HipProtectionService
import testdata.HipTestData.{amendProtectionRequest, amendProtectionResponse, lifetimeAllowanceIdentifier}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.util.Random
import scala.concurrent.{ExecutionContext, Future}

class HipAmendProtectionsControllerSpec
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

  private val controller = new HipAmendProtectionsController(
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

  private val validAmendRequestBody = Json.toJson(amendProtectionRequest)

  private val invalidAmendRequestBody = Json.parse(
    s"""{
       |  "incorrectField": "IP2016"
       |}
       |""".stripMargin
  )

  "HipAmendProtectionsController on amendProtection" should {

    "call HipProtectionService" in {
      when(hipProtectionService.amendProtection(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(amendProtectionResponse)))

      val request = FakeRequest(
        method = "POST",
        uri = "/",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validAmendRequestBody
      )

      controller.amendProtection(testNino, lifetimeAllowanceIdentifier)(request).futureValue

      verify(hipProtectionService).amendProtection(
        eqTo(testNino),
        eqTo(lifetimeAllowanceIdentifier),
        eqTo(amendProtectionRequest)
      )(any())
    }

    "return Ok when provided with correct request" in {
      when(hipProtectionService.amendProtection(any(), any(), any())(any()))
        .thenReturn(Future.successful(Right(amendProtectionResponse)))

      val request = FakeRequest(
        method = "POST",
        uri = "/",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validAmendRequestBody
      )

      val result = controller.amendProtection(testNino, lifetimeAllowanceIdentifier)(request)

      status(result) shouldBe OK
      contentAsJson(result).as[AmendProtectionResponse] shouldBe amendProtectionResponse
    }

    "return Bad Request when provided with incorrect request" in {
      val request = FakeRequest(
        method = "POST",
        uri = "/",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = invalidAmendRequestBody
      )

      val result = controller.amendProtection(testNino, lifetimeAllowanceIdentifier)(request)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("body failed validation with error: ")
    }

    "return Internal Server Error" when
      Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { errorStatus =>
        s"HipProtectionService returns Left containing UpstreamErrorResponse with status: $errorStatus" in {
          val testException = UpstreamErrorResponse("Test Exception", errorStatus)
          when(hipProtectionService.amendProtection(any(), any(), any())(any()))
            .thenReturn(Future.successful(Left(testException)))

          val request = FakeRequest(
            method = "POST",
            uri = "/",
            headers = FakeHeaders(Seq("content-type" -> "application.json")),
            body = validAmendRequestBody
          )

          val result = controller.amendProtection(testNino, lifetimeAllowanceIdentifier)(request)

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result) should include("Test Exception")
        }
      }
  }

}

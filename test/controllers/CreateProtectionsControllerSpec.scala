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

package controllers

import _root_.mock.AuthMock
import connectors.{CitizenDetailsConnector, CitizenRecordOK, NpsConnector}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.mvc._
import util.{TestUtils, WithFakeApplication}
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.ProtectionService
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

class CreateProtectionsControllerSpec
    extends PlaySpec
    with BeforeAndAfterEach
    with AuthMock
    with GuiceOneServerPerSuite
    with WithFakeApplication
    with TestUtils {

  private implicit val system: ActorSystem = ActorSystem("test-sys")

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]

  when(
    mockCitizenDetailsConnector
      .checkCitizenRecord(ArgumentMatchers.any[String])(ArgumentMatchers.any(), ArgumentMatchers.any())
  )
    .thenReturn(Future.successful(CitizenRecordOK))

  mockAuthConnector(Future.successful {})

  implicit lazy val cc: ControllerComponents     = app.injector.instanceOf[ControllerComponents]
  val mockService: ProtectionService             = mock[ProtectionService]
  val mockNPSResponseHandler: NPSResponseHandler = mock[NPSResponseHandler]
  implicit val ec: ExecutionContext              = app.injector.instanceOf[ExecutionContext]

  val controller: CreateProtectionsController = new CreateProtectionsController(
    mockAuthConnector,
    citizenDetailsConnector = mockCitizenDetailsConnector,
    mockService,
    cc
  )

  val mockNpsConnector: NpsConnector            = mock[NpsConnector]
  val mockHandleNpsResponse: NPSResponseHandler = mock[NPSResponseHandler]

  val validApplicationBody: JsValue = Json.parse(
    """
      |{
      |  "protectionType" : "FP2016"
      |}
    """.stripMargin
  )

  val invalidApplicationBody: JsValue = Json.parse("""
                                                     |{
                                                     |  "type" : 100000
                                                     |}
    """.stripMargin)

  val successfulCreateFP2016NPSResponseBody: JsObject = Json
    .parse(s"""
              |  {
              |      "nino": "$testNinoWithoutSuffix",
              |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
              |      "protection": {
              |        "id": 1,
              |        "version": 1,
              |        "type": 1,
              |        "certificateDate": "2015-05-22",
              |        "certificateTime": "12:22:59",
              |        "status": 1,
              |        "protectionReference": "FP161234567890C",
              |        "relevantAmount": 1250000.00,
              |        "notificationID": 12
              |      }
              |    }
              |
    """.stripMargin)
    .as[JsObject]

  val unsuccessfulCreateFP2016NPSResponseBody: JsObject = Json
    .parse(s"""
              |  {
              |      "nino": "$testNinoWithoutSuffix",
              |      "protection": {
              |        "id": 1,
              |        "version": 1,
              |        "type": 2,
              |        "status": 5,
              |        "notificationID": 10
              |      }
              |  }
    """.stripMargin)
    .as[JsObject]

  val standardHeaders: Seq[(String, Seq[String])] = Seq("Content-type" -> Seq("application/json"))

  val validExtraHOutboundHeaders: Seq[(String, Seq[String])] =
    Seq("Environment" -> Seq("local"), "Authorisation" -> Seq("Bearer abcdef12345678901234567890"))

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
    block(request)

  "CreateProtectionController" when {
    "respond to a valid Create Protection request with OK" in {
      when(
        mockService.applyForProtection(ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulCreateFP2016NPSResponseBody))))

      lazy val fakeRequest: FakeRequest[JsValue] = FakeRequest(
        method = "POST",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validApplicationBody
      )

      lazy val result: Future[Result] = controller.applyForProtection(testNino).apply(fakeRequest)
      status(result) shouldBe OK
    }

    "handle a 400 (BAD_REQUEST) response from NPS service by passing it back to the caller" in {
      when(
        mockService.applyForProtection(ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      )
        .thenReturn(Future.failed(new BadRequestException("bad request")))

      lazy val fakeRequest = FakeRequest(
        method = "POST",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validApplicationBody
      )

      lazy val result = controller.applyForProtection(testNino).apply(fakeRequest)
      status(result) shouldBe BAD_REQUEST
    }

    "handle an invalid Json submission" in {
      lazy val fakeRequest = FakeRequest(
        method = "POST",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = invalidApplicationBody
      )

      lazy val result = controller.applyForProtection(testNino).apply(fakeRequest)
      status(result) shouldBe BAD_REQUEST
      await(jsonBodyOf(result)) shouldBe Json.obj(
        "message" -> JsString(
          "body failed validation with errors: List((/protectionType,List(JsonValidationError(List(error.path.missing),List()))))"
        )
      )
    }
  }

}

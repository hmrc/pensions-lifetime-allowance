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

import config.AppConfig
import connectors.NpsResponseHandler.notFoundMessage
import connectors.{HipConnector, NpsConnector}
import model._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsSuccess, JsValue, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import util.{NinoHelper, WithFakeApplication}

import java.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProtectionServiceSpec
    extends PlaySpec
    with GuiceOneServerPerSuite
    with WithFakeApplication
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach
    with ScalaCheckDrivenPropertyChecks {

  val rand               = new Random()
  val ninoGenerator      = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino: String           = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  val mockNpsConnector: NpsConnector = mock[NpsConnector]
  val hipConnector: HipConnector     = mock[HipConnector]
  val appConfig: AppConfig           = mock[AppConfig]

  val service: ProtectionService = new DefaultProtectionService(mockNpsConnector, hipConnector, appConfig)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val protectionId = 1L
  private val version      = 1

  val amendment: ProtectionAmendment = ProtectionAmendment(
    "IP2016",
    1,
    "Open",
    1000.0,
    2000.0,
    3000.0,
    4000.0,
    5000.0,
    Some(6000.0),
    Some(List(PensionDebit("2016-04-04", 1001.0), PensionDebit("2016-05-05", 1002.0))),
    Some("2017-04-04")
  )

  val validAmendBody: JsValue = Json.parse(
    s"""
       |{
       | "protectionType": "IP2016",
       | "status": "Open",
       | "version": $protectionId,
       | "relevantAmount": 1250000.00,
       | "postADayBenefitCrystallisationEvents": 250000.00,
       | "preADayPensionInPayment": 250000.00,
       | "nonUKRights": 250000.00,
       | "uncrystallisedRights": 500000.00
       |}
    """.stripMargin
  )

  val amendResponse: JsValue = Json
    .parse(s"""
              |  {
              |      "nino": "$testNinoWithoutSuffix",
              |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
              |      "protection": {
              |        "id": $protectionId,
              |        "version": $protectionId,
              |        "type": 1,
              |        "certificateDate": "2015-05-22",
              |        "certificateTime": "12:22:59",
              |        "status": 1,
              |        "protectionReference": "IP161234567890C",
              |        "relevantAmount": 1250000.00,
              |        "notificationID": 12
              |      }
              |    }
              |
    """.stripMargin)

  val protectionModel: ProtectionModel = ProtectionModel(testNinoWithoutSuffix, protectionType = "IP2016")

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNpsConnector)
    reset(hipConnector)
    reset(appConfig)
  }

  "ProtectionService" when {

    ".amendProtection is called" must {

      val requestJson = validAmendBody
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "nino"       -> testNinoWithoutSuffix,
            "protection" -> Json.obj("id" -> protectionId)
          )
        )

      "call HipConnector when AppConfig.isHipApiEnabled is true" in {
        when(appConfig.isHipApiEnabled).thenReturn(true)
        when(hipConnector.amendProtection())
          .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(JsObject.empty))))

        service.amendProtection(testNino, protectionId, requestJson).futureValue

        verify(hipConnector).amendProtection()
        verifyNoInteractions(mockNpsConnector)
      }

      "call NPSConnector when AppConfig.isHipApiEnabled is false" in {
        when(appConfig.isHipApiEnabled).thenReturn(false)
        when(mockNpsConnector.amendProtection(eqTo(testNino), any(), eqTo(requestJson))(any(), any()))
          .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(amendResponse.as[JsObject]))))

        service.amendProtection(testNino, protectionId, requestJson).futureValue

        verify(mockNpsConnector).amendProtection(eqTo(testNino), eqTo(protectionId), eqTo(requestJson))(any(), any())
        verifyNoInteractions(hipConnector)
      }

      "return 200 with a protection model created from the NPS payload" when {

        "NPS Connector returns 200 with a valid payload" in {

          when(mockNpsConnector.amendProtection(eqTo(testNino), any(), eqTo(requestJson))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(amendResponse.as[JsObject]))))

          val result: Future[HttpResponseDetails] = service.amendProtection(testNino, protectionId, requestJson)

          result.futureValue.status mustBe OK
          result.futureValue.body mustBe JsSuccess(
            Json
              .toJson[ProtectionModel](
                ProtectionModel(
                  nino = testNino,
                  psaCheckReference = Some("PSA123456789"),
                  protectionID = Some(protectionId),
                  certificateDate = Some("2015-05-22T12:22:59"),
                  version = Some(1),
                  protectionType = "FP2016",
                  status = Some("Open"),
                  relevantAmount = Some(1250000),
                  notificationId = Some(12),
                  protectionReference = Some("IP161234567890C")
                )
              )
              .as[JsObject]
          )
        }
      }

      "return 400 and a JsError" when {

        "NPS Connector returns a 400 error when ninos do not match" in {

          val npsResponseBody =
            JsSuccess(
              Json.toJsObject(Error(s"Received nino $randomNino is not same as sent nino $testNinoWithoutSuffix"))
            )

          when(mockNpsConnector.amendProtection(eqTo(testNino), any(), eqTo(requestJson))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(BAD_REQUEST, npsResponseBody)))

          val result: Future[HttpResponseDetails] = service.amendProtection(testNino, protectionId, requestJson)

          result.futureValue.status mustBe BAD_REQUEST
          result.futureValue.body.isError mustBe true
        }
      }

      "throw a NotFoundException" when {

        "NPS Connector throws a NotFoundException upon processing a 4xx status code" in {

          when(mockNpsConnector.amendProtection(eqTo(testNino), any(), eqTo(requestJson))(any(), any()))
            .thenReturn(
              Future.failed(
                new NotFoundException(
                  notFoundMessage(
                    "PUT",
                    s"/pensions-lifetime-allowance/individual/$testNinoWithoutSuffix/protections/$protectionId",
                    "Something went wrong"
                  )
                )
              )
            )

          recoverToSucceededIf[NotFoundException] {
            service.amendProtection(testNino, protectionId, requestJson)
          }
        }
      }

      "return 500 and a JsError" when {

        "NPS Connector returns a 500 error with other JSON" in {

          val npsResponseBody =
            JsSuccess(
              Json.toJsObject(Error("Something went wrong"))
            )

          when(mockNpsConnector.amendProtection(eqTo(testNino), any(), eqTo(requestJson))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(INTERNAL_SERVER_ERROR, npsResponseBody)))

          val result: Future[HttpResponseDetails] = service.amendProtection(testNino, protectionId, requestJson)

          result.futureValue.status mustBe INTERNAL_SERVER_ERROR
          result.futureValue.body.isError mustBe true
        }
      }
    }

    ".readExistingProtections is called" must {

      "return 200 with a protection model created from the NPS payload" when {

        "NPS Connector returns 200 with no protections" in {

          val existingProtectionsPayload =
            Json
              .parse(s"""
                        |{
                        | "nino": "$testNinoWithoutSuffix",
                        | "pensionSchemeAdministratorCheckReference": "PSA123456789",
                        | "protections": []
                        |}
        """.stripMargin)
              .as[JsObject]

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(existingProtectionsPayload))))

          val result: Future[HttpResponseDetails] = service.readExistingProtections(testNino)

          result.futureValue.status mustBe OK
          result.futureValue.body mustBe JsSuccess(
            Json
              .toJson[ReadProtectionsModel](
                ReadProtectionsModel(testNino, "PSA123456789", Some(List.empty))
              )
              .as[JsObject]
          )
        }

        "NPS Connector returns 200 with a single protection" in {

          val existingProtectionsPayload =
            Json
              .parse(s"""
                        |{
                        | "nino": "$testNinoWithoutSuffix",
                        | "pensionSchemeAdministratorCheckReference" : "PSA123456789",
                        | "protections" :  [
                        |   {
                        |     "id": 1,
                        |     "version": 1,
                        |     "type": 1,
                        |     "status": 1
                        |   }
                        | ]
                        |}
        """.stripMargin)
              .as[JsObject]

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(existingProtectionsPayload))))

          val result: Future[HttpResponseDetails] = service.readExistingProtections(testNino)

          result.futureValue.status mustBe OK
          result.futureValue.body mustBe JsSuccess(
            Json
              .toJson[ReadProtectionsModel](
                ReadProtectionsModel(
                  testNino,
                  "PSA123456789",
                  Some(
                    List(ReadProtection(protectionId, version = version, protectionType = "FP2016", status = "Open"))
                  )
                )
              )
              .as[JsObject]
          )
        }

        "NPS Connector returns 200 with multiple protections" in {

          val existingProtectionsPayload =
            Json
              .parse(s"""
                        |{
                        | "nino": "$testNinoWithoutSuffix",
                        | "pensionSchemeAdministratorCheckReference" : "PSA123456789",
                        | "protections" : [
                        |   {
                        |     "id": 1,
                        |     "version": 1,
                        |     "type": 1,
                        |     "status": 1
                        |   },
                        |   {
                        |     "id": 2,
                        |     "version": 1,
                        |     "type": 1,
                        |     "status": 3
                        |   }
                        | ]
                        |}
        """.stripMargin)
              .as[JsObject]

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(existingProtectionsPayload))))

          val result: Future[HttpResponseDetails] = service.readExistingProtections(testNino)

          result.futureValue.status mustBe OK
          result.futureValue.body mustBe JsSuccess(
            Json
              .toJson[ReadProtectionsModel](
                ReadProtectionsModel(
                  testNino,
                  "PSA123456789",
                  Some(
                    List(
                      ReadProtection(protectionId, version = version, protectionType = "FP2016", status = "Open"),
                      ReadProtection(2, version = version, protectionType = "FP2016", status = "Withdrawn")
                    )
                  )
                )
              )
              .as[JsObject]
          )
        }
      }

      "return 400 and a JsError" when {

        "NPS Connector returns a 400 error when ninos do not match" in {

          val npsResponseBody =
            JsSuccess(
              Json.toJsObject(Error(s"Received nino $randomNino is not same as sent nino $testNinoWithoutSuffix"))
            )

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(BAD_REQUEST, npsResponseBody)))

          val result: Future[HttpResponseDetails] = service.readExistingProtections(testNino)

          result.futureValue.status mustBe BAD_REQUEST
          result.futureValue.body.isError mustBe true
        }
      }

      "throw a NotFoundException" when {

        "NPS Connector throws a NotFoundException upon processing a 4xx status code" in {

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(
              Future.failed(
                new NotFoundException(
                  notFoundMessage(
                    "PUT",
                    s"/pensions-lifetime-allowance/individual/$testNinoWithoutSuffix/protections",
                    "Something went wrong"
                  )
                )
              )
            )

          recoverToSucceededIf[NotFoundException] {
            service.readExistingProtections(testNino)
          }
        }
      }

      "return 500 and a JsError" when {

        "NPS Connector returns a 500 error with other JSON" in {

          val npsResponseBody =
            JsSuccess(
              Json.toJsObject(Error("Something went wrong"))
            )

          when(mockNpsConnector.readExistingProtections(eqTo(testNino))(any(), any()))
            .thenReturn(Future.successful(HttpResponseDetails(INTERNAL_SERVER_ERROR, npsResponseBody)))

          val result: Future[HttpResponseDetails] = service.readExistingProtections(testNino)

          result.futureValue.status mustBe INTERNAL_SERVER_ERROR
          result.futureValue.body.isError mustBe true
        }
      }
    }
  }

}

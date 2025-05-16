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

package connectors

import model.Error
import org.scalatest.RecoverMethods.recoverToSucceededIf
import play.api.http.Status._
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import util._
import play.api.libs.json._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utilities.IntegrationSpec

import java.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class NPSConnectorISpec extends IntegrationSpec {

  val npsConnector: DefaultNpsConnector = app.injector.instanceOf[DefaultNpsConnector]

  val rand               = new Random()
  val ninoGenerator      = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino: String           = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  val id = 1

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "NPSConnector" when {

    ".amendProtection is called" must {

      def submissionJson(nino: String = testNinoWithoutSuffix): JsValue =
        Json
          .parse(s"""
                    |{
                    |  "nino" : "$nino",
                    |  "pensionDebits": [
                    |    {
                    |      "pensionDebitStartDate": "2016-04-04",
                    |      "pensionDebitEnteredAmount": 1001.0
                    |    }
                    |  ],
                    |  "protection" : {
                    |    "type" : $id,
                    |    "status" : -1,
                    |    "version" : 1,
                    |    "relevantAmount" : 10000,
                    |    "postADayBCE" : 1000,
                    |    "preADayPensionInPayment" : 1000,
                    |    "uncrystallisedRights" : 1000,
                    |    "nonUKRights" : 1000,
                    |    "id" : 1
                    |  }
                    |}
      """.stripMargin)

      val npsResponse =
        Json
          .parse(s"""
                    |  {
                    |      "nino": "$testNinoWithoutSuffix",
                    |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
                    |      "protection": {
                    |        "id": $id,
                    |        "version": $id,
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

      val amendJson: JsObject = submissionJson()
        .as[JsObject]
        .deepMerge(
          Json.obj(
            "nino"       -> testNinoWithoutSuffix,
            "protection" -> Json.obj("id" -> id)
          )
        )

      "return a successful response" when {

        "response nino matches given nino" in {

          mockAmend(testNinoWithoutSuffix, OK, npsResponse.toString(), id)

          val result = npsConnector.amendProtection(testNino, id, amendJson).futureValue

          result.status mustBe OK
          result.body mustBe JsSuccess(npsResponse.as[JsObject])
        }
      }

      "return a 400 status response" when {

        "the nino in the response does not match the request nino" in {

          val mismatchNino = randomNino

          mockAmend(testNinoWithoutSuffix, OK, submissionJson(mismatchNino).toString(), id)

          val result = npsConnector.amendProtection(testNinoWithoutSuffix, id, amendJson).futureValue

          result.status mustBe BAD_REQUEST
          result.body mustBe JsSuccess(
            Json
              .toJson(
                Error(
                  s"Received nino $mismatchNino is not same as sent nino $testNinoWithoutSuffix"
                )
              )
              .as[JsObject]
          )
        }
      }

      "throw a NotFoundException" when {

        "NPS returns a 400 to the connector" in {

          val response = """{"message":"Something went wrong"}"""

          mockAmend(testNinoWithoutSuffix, BAD_REQUEST, response, id)

          recoverToSucceededIf[NotFoundException] {
            npsConnector.amendProtection(testNino, id, amendJson)
          }
        }
      }

      "return the corresponding response code" when
        Seq(CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { code =>
          s"NPS returns a $code to the connector" in {

            mockAmend(testNinoWithoutSuffix, code, npsResponse.toString(), id)

            npsConnector.amendProtection(testNino, id, amendJson).futureValue.status mustBe code
          }
        }
    }

    ".getPSALookup is called" must {

      val psaRef = "PSA12345678A"
      val ltaRef = "IP141000000000A"

      "return 200 with expected result" when {

        "NPS returns 200 with valid payload" in {

          val validResponse: JsValue = Json.parse(
            s"""{
               |"pensionSchemeAdministratorCheckReference": "PSA12345678A",
               |"ltaType": 7,"psaCheckResult": 1,
               |"relevantAmount": 25000
               |}""".stripMargin
          )

          stubPSALookup(psaRef, ltaRef, OK, validResponse.toString())

          val result = npsConnector.getPSALookup(psaRef, ltaRef).futureValue

          result.status mustBe OK
          result.json mustBe validResponse
        }
      }

      s"throw a NotFoundException" when
        Seq(BAD_REQUEST, NOT_FOUND).foreach { code =>
          s"NPS returns a $code status code and error" in {

            stubPSALookup(psaRef, ltaRef, code, s"""{"message":"NPS returned a $code error"}""")

            recoverToSucceededIf[NotFoundException] {
              npsConnector.getPSALookup(psaRef, ltaRef)
            }
          }
        }

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { code =>
        s"return $code with error message" when {

          s"NPS returns a $code status code and error" in {

            val errorBody = s"""{"message":"NPS returned a $code error"}"""

            stubPSALookup(psaRef, ltaRef, code, errorBody)

            val result = npsConnector.getPSALookup(psaRef, ltaRef).futureValue

            result.status mustBe code
            result.body mustBe errorBody
          }
        }
      }
    }

    ".readExistingProtections is called" must {

      "return 200 with expected result" when {

        "NPS returns 200 with valid payload" in {

          val validResponse =
            Json
              .parse(s"""
                        |{
                        | "nino": "$testNinoWithoutSuffix",
                        | "pensionSchemeAdministratorCheckReference": "PSA123456789",
                        | "protections": []
                        |}
        """.stripMargin)
              .as[JsObject]

          stubReadExistingProtections(testNinoWithoutSuffix, OK, validResponse.toString())

          val result = npsConnector.readExistingProtections(testNino).futureValue

          result.status mustBe OK
          result.body mustBe JsSuccess(validResponse)
        }
      }

      "return a 400 status response" when {

        "the nino in the response does not match the request nino" in {

          val mismatchNino = randomNino

          val validResponse =
            Json
              .parse(s"""
                        |{
                        | "nino": "$mismatchNino",
                        | "pensionSchemeAdministratorCheckReference": "PSA123456789",
                        | "protections": []
                        |}
        """.stripMargin)
              .as[JsObject]

          stubReadExistingProtections(testNinoWithoutSuffix, OK, validResponse.toString())

          val result = npsConnector.readExistingProtections(testNino).futureValue

          result.status mustBe BAD_REQUEST
          result.body mustBe JsSuccess(
            Json
              .toJson(
                Error(
                  s"Received nino $mismatchNino is not same as sent nino $testNinoWithoutSuffix"
                )
              )
              .as[JsObject]
          )
        }

        Seq(CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { code =>
          s"NPS returns a $code to the connector" in {

            stubReadExistingProtections(testNinoWithoutSuffix, code, """{"message":"Something went wrong"}""")

            val result = npsConnector.readExistingProtections(testNino).futureValue

            result.status mustBe BAD_REQUEST
            result.body mustBe JsSuccess(
              Json
                .toJson(
                  Error(
                    s"Received nino  is not same as sent nino $testNinoWithoutSuffix"
                  )
                )
                .as[JsObject]
            )
          }
        }
      }

      "throw a NotFoundException" when {

        "NPS returns a 400 to the connector" in {

          stubReadExistingProtections(testNinoWithoutSuffix, BAD_REQUEST, """{"message":"Something went wrong"}""")

          recoverToSucceededIf[NotFoundException] {
            npsConnector.readExistingProtections(testNino)
          }
        }
      }
    }
  }

}

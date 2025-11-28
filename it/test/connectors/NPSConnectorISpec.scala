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

import org.scalatest.RecoverMethods.recoverToSucceededIf
import play.api.http.Status._
import util._
import play.api.libs.json._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}
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

            recoverToSucceededIf[UpstreamErrorResponse] {
              npsConnector.getPSALookup(psaRef, ltaRef)
            }
          }
        }
      }
    }
  }

}

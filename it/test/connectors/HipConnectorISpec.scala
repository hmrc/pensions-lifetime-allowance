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

import com.github.tomakehurst.wiremock.client.WireMock._
import model.hip.{AmendProtectionResponse, ReadExistingProtectionsResponse, UpdatedLifetimeAllowanceProtectionRecord}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utilities.IntegrationSpec

import scala.concurrent.ExecutionContext.Implicits.global

class HipConnectorISpec extends IntegrationSpec {

  private val hipConnector = app.injector.instanceOf[HipConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val amendProtectionResponseJson =
    Json.parse(s"""{
                  |  "updatedLifetimeAllowanceProtectionRecord": {
                  |    "identifier": 42
                  |  }
                  |}""".stripMargin)

  private val readExistingProtectionsResponseJson =
    Json.parse(s"""{
                  |  "pensionSchemeAdministratorCheckReference": "PSA12345678A"
                  |}""".stripMargin)

  "HipConnector on amendProtection" must {

    "call correct HIP endpoint" in {
      stubPost(
        url = "/amend",
        status = 200,
        responseBody = amendProtectionResponseJson.toString
      )

      hipConnector.amendProtection().futureValue

      verify(postRequestedFor(urlEqualTo("/amend")))
    }

    "return response from HIP" in {
      stubPost(
        url = "/amend",
        status = 200,
        responseBody = amendProtectionResponseJson.toString
      )

      val result = hipConnector.amendProtection().futureValue

      result mustBe AmendProtectionResponse(UpdatedLifetimeAllowanceProtectionRecord(42))
    }
  }

  "HipConnector on readExistingProtections" must {

    "call correct HIP endpoint" in {
      stubGet(
        url = "/read",
        status = 200,
        body = readExistingProtectionsResponseJson.toString
      )

      hipConnector.readExistingProtections().futureValue

      verify(getRequestedFor(urlEqualTo("/read")))
    }

    "return response from HIP" in {
      stubGet(
        url = "/read",
        status = 200,
        body = readExistingProtectionsResponseJson.toString
      )

      val result = hipConnector.readExistingProtections().futureValue

      result mustBe ReadExistingProtectionsResponse("PSA12345678A")
    }
  }

}

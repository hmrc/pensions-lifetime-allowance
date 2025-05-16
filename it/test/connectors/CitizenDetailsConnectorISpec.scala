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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlPathMatching}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.http.client.HttpClientV2
import util.{TestUtils, WithFakeApplication}
import utilities.{IntegrationSpec, WiremockHelperIT}

import scala.concurrent.ExecutionContext.Implicits.global

class CitizenDetailsConnectorISpec
    extends IntegrationSpec
    with MockitoSugar
    with BeforeAndAfter
    with TestUtils
    with WithFakeApplication
    with WiremockHelperIT {

  private val DefaultTestNino       = "KA191435A"
  private val DesignatoryDetailsUrl = s"/citizen-details/$DefaultTestNino/designatory-details"
  private val DefaultLocalUrl       = url

  def bindModules: Seq[GuiceableModule] = Seq()

  object testCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl = DefaultLocalUrl

    override def http: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
    override val checkRequired      = true
  }

  object NoCheckRequiredCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl         = DefaultLocalUrl
    override def http: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
    override val checkRequired      = false
  }

  "The CitizenDetails Connector getCitizenRecordCheckUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      testCitizenDetailsConnector.getCitizenRecordCheckUrl(DefaultTestNino).contains(DefaultTestNino) shouldBe true
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" when {
    "return a CitizenRecordOK response when no check is needed" in {
      val f = NoCheckRequiredCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" when {

    "return a valid HTTPResponse for successful retrieval" in {
      wireMockServer.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }

    "return an error if NotFoundException received" in {

      wireMockServer.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordNotFound
    }

    "return an error if Upstream4xxResponse received" in {

      wireMockServer.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res.isInstanceOf[CitizenRecordOther4xxResponse] shouldBe true
    }

    "return an error if Upstream5xxResponse received" in {

      wireMockServer.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res.isInstanceOf[CitizenRecord5xxResponse] shouldBe true
    }

    "return an error if CitizenRecordLocked received" in {

      wireMockServer.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(LOCKED)
          )
      )
      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordLocked
    }
  }

}

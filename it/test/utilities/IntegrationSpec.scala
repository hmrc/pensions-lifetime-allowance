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

package utilities

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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, stubFor}
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import java.util

trait IntegrationSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with Matchers
    with WiremockHelperIT
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopWiremock()
  }

  def mockAmend(nino: String, status: Int, body: String, id: Long): StubMapping = {
    val url = s"/pensions-lifetime-allowance/individual/$nino/protections/$id"
    stubPut(url, status, body)
  }

  def stubPSALookup(psaRef: String, ltaRef: String, status: Int, body: String): StubMapping = {

    val url =
      s"/pensions-lifetime-allowance/scheme-administrator/certificate-lookup?pensionSchemeAdministratorCheckReference=$psaRef&lifetimeAllowanceReference=$ltaRef"

    def lookupQueryParams: util.Map[String, StringValuePattern] = {
      import scala.jdk.CollectionConverters._

      Map(
        "pensionSchemeAdministratorCheckReference" -> equalTo(psaRef),
        "lifetimeAllowanceReference"               -> equalTo(ltaRef)
      ).asJava
    }

    stubFor(
      get(url)
        .withQueryParams(lookupQueryParams)
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(body)
        )
    )
  }

  def stubReadExistingProtections(nino: String, status: Int, body: String): StubMapping =
    stubGet(s"/pensions-lifetime-allowance/individual/$nino/protections", status, body)

}

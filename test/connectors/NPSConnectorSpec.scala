/*
 * Copyright 2016 HM Revenue & Customs
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

import java.util.Random

import util._
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.http.{HeaderCarrier,HttpResponse}
import config.WSHttp
import uk.gov.hmrc.domain.Generator


class NPSConnectorSpec extends UnitSpec{

  object testNPSConnector extends NpsConnector {
    override val serviceUrl = "http://localhost:80"
    override def http = WSHttp

    override val serviceAccessToken = "token"
    override val serviceEnvironment = "environment"
  }

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino = randomNino
  val (testNinoWithoutSuffix,_) = NinoHelper.dropNinoSuffix(testNino)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "The NPS connector implicit header carrier  " should {
    "should have the environment and authorisation headers set" in {
      testNPSConnector.addExtraHeaders.headers.find(_._1 == "Environment").isDefined shouldBe true
      testNPSConnector.addExtraHeaders.headers.find(_._1 == "Authorization").isDefined shouldBe true
    }
  }

  "The  NPS Conector response handler" should {
    "handle 409 responses as successes and pass the status back unmodifed" in {
      val handledHttpResponse =  NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(409))
      handledHttpResponse.status shouldBe 409
    }
  }

  "The  NPS Connector response handler" should {
    "handle non-OK responses other than 409 as failures and throw an exception" in {
      try {
        val handledHttpResponse =  NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(400))
        fail("Exception not thrown")
      } catch {
        case ex =>
      }
    }
  }

  "The NPS COnnector getUrl method" should {
    "return a  URL that contains the nino passed to it" in {
      testNPSConnector.getUrl(testNinoWithoutSuffix).contains(testNinoWithoutSuffix) shouldBe true
    }
  }
}

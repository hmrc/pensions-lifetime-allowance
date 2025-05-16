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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse
import util.{NinoHelper, TestUtils, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class NPSConnectorSpec extends PlaySpec with WithFakeApplication with TestUtils {

  val npsConnector: NpsConnector = fakeApplication().injector.instanceOf[NpsConnector]

  "The NPS connector implicit header carrier  " when {
    "should have the environment and authorisation headers set" in {
      npsConnector.httpHeaders().exists(_._1 == "Environment") mustBe true
      npsConnector.httpHeaders().exists(_._1 == "Authorization") mustBe true
    }
  }

  "The  NPS Connector response handler" when {
    "handle 409 responses as successes and pass the status back unmodifed" in {
      val handledHttpResponse = NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(409, ""))
      handledHttpResponse.status mustBe 409
    }
  }

  "The NPS Connector response handler" when {
    "handle non-OK responses other than 409 as failures and throw an exception" in {
      try {
        NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(400, ""))
        fail("Exception not thrown")
      } catch {
        case ex: Throwable =>
      }
    }
  }

  "The NPS Connector getAmendUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      npsConnector.getAmendUrl(testNinoWithoutSuffix, 1).contains(testNinoWithoutSuffix) mustBe true
    }
  }

  "The NPS Connector getReadUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      npsConnector.getReadUrl(testNinoWithoutSuffix).contains(testNinoWithoutSuffix) mustBe true
    }
  }

  "The NPS Connector handleAuditableResponse" when {
    "return a HTTPResponseDetails object with valid fields" in {
      val requestStr =
        s"""
           |{
           | "nino": "$testNinoWithoutSuffix",
           | "protection": {
           |   "type": 1
           |   }
           | }
      """.stripMargin
      val responseBody = Json.parse(requestStr).as[JsObject]
      val responseDetails =
        npsConnector.handleAuditableResponse(testNino, HttpResponse(200, responseBody.toString()), None)
      responseDetails.status mustBe 200
      responseDetails.body.isSuccess mustBe true
    }
  }

  "The NPS Connector handleAuditableResponse" when {
    "return a HTTPResponseDetails object with a 400 status if the nino returned differs from that sent" in {
      val (t1NinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(randomNino)
      val (t2NinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(randomNino)

      val requestStr =
        s"""
           |{
           | "nino": "$t1NinoWithoutSuffix",
           | "protection": {
           |   "type": 1
           |   }
           | }
      """.stripMargin
      val responseBody = Json.parse(requestStr).as[JsObject]
      val responseDetails =
        npsConnector.handleAuditableResponse(testNino, HttpResponse(200, responseBody.toString()), None)
      responseDetails.status mustBe 400
      responseDetails.body.isSuccess mustBe true
    }
  }

  "The NPS Connector handleEResponse" when {
    "return a HTTPResponseDetails object with valid fields" in {
      val responseStr =
        s"""
        {
           |"nino": "$testNinoWithoutSuffix",
           | "protection": {
           |   "type": 1
           |  }
           |}
      """.stripMargin
      val responseDetails = npsConnector.handleExpectedReadResponse(testNino, HttpResponse(200, responseStr))
      responseDetails.status mustBe 200
      responseDetails.body.isSuccess mustBe true
    }
  }

  "The NPS Connector handleExpectedReadResponse" when {
    "return a HTTPResponseDetails object with a 400 status if the nino returned differs from that sent" in {
      val (t1NinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(randomNino)

      val requestStr =
        s"""
           |{
           | "nino": "$t1NinoWithoutSuffix"
           | }
      """.stripMargin

      val responseDetails = npsConnector.handleExpectedReadResponse(testNino, HttpResponse(200, requestStr))
      responseDetails.status mustBe 400
      responseDetails.body.isSuccess mustBe true
    }
  }

}

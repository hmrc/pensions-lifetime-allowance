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

import org.apache.pekko.actor.ActorSystem
import model.HttpResponseDetails
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import util.TestUtils
import play.api.libs.json._
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

class NPSResponseHandlerSpec extends PlaySpec with TestUtils {

  val testResponseHandler: NPSResponseHandler = new NPSResponseHandler {}

  private implicit val system: ActorSystem = ActorSystem("test-sys")

  "NPSResponseHandler" when {

    "handle a NPS error" when {
      "a Service unavailable response is received" in {
        val npsError = UpstreamErrorResponse("service unavailable", SERVICE_UNAVAILABLE, 1)
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe SERVICE_UNAVAILABLE
      }
      "a Bad gateway response is received" in {
        val npsError = UpstreamErrorResponse("bad gateway", BAD_GATEWAY, 1)
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Unauthorized response is received" in {
        val npsError = UpstreamErrorResponse("unauthorized", UNAUTHORIZED, 1)
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe UNAUTHORIZED
      }
      "a Forbidden response is received" in {
        val npsError = UpstreamErrorResponse("forbidden", FORBIDDEN, 1)
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "a Bad request response is received" in {
        val npsError = new BadRequestException("bad request")
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe BAD_REQUEST
      }
      "a Not found response is received" in {
        val npsError = new NotFoundException("not found")
        val result   = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe NOT_FOUND
      }
      "a different error is thrown" in {
        val npsError = new RuntimeException("different error")
        a[RuntimeException] shouldBe thrownBy(
          testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        )
      }
    }
  }

}

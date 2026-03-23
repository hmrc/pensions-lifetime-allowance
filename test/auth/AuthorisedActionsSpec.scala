/*
 * Copyright 2026 HM Revenue & Customs
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

package auth

import connectors.CitizenDetailsConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Results.Status
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, InvalidBearerToken}

import scala.concurrent.Future

class AuthorisedActionsSpec extends AnyWordSpec with Matchers with ScalaFutures {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]

  object TestAuthorisedActions extends AuthorisedActions with Results {
    override val citizenDetailsConnector: CitizenDetailsConnector = mockCitizenDetailsConnector
    override val authConnector                                    = null
  }

  "logErrorAndRespond" should {

    "return the provided status with the error message" in {

      val result: Future[Result] =
        TestAuthorisedActions.logErrorAndRespond("test error", Status(BAD_REQUEST))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("test error")
    }
  }

  "logErrorAndRespondFromUpstreamResponse" should {

    "return the provided status with upstream error message" in {

      val upstreamError = new RuntimeException("upstream failure")

      val result =
        TestAuthorisedActions
          .logErrorAndRespondFromUpstreamResponse("error occurred", Status(BAD_GATEWAY), upstreamError)

      status(result) shouldBe BAD_GATEWAY
      contentAsString(result) should include("upstream failure")
    }
  }

  "authErrorHandling" should {

    "return Unauthorized when NoActiveSession occurs" in {

      val result =
        TestAuthorisedActions.authErrorHandling(InvalidBearerToken("invalid bearer token"))

      result.header.status shouldBe UNAUTHORIZED
    }

    "return Forbidden when AuthorisationException occurs" in {

      val result =
        TestAuthorisedActions.authErrorHandling(new InsufficientEnrolments)

      result.header.status shouldBe FORBIDDEN
    }
  }

}

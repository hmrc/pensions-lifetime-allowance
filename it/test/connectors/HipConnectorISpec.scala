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
import model.hip.{AmendProtectionLifetimeAllowanceType, AmendProtectionResponseStatus, ReadExistingProtectionsResponse}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import testdata.HipTestData._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import util.{IdGenerator, TestUtils}
import utilities.IntegrationSpec

import java.util.UUID
import scala.concurrent.Future

class HipConnectorISpec extends IntegrationSpec {

  private val auditConnector = mock[AuditConnector]
  private val idGenerator    = mock[IdGenerator]

  override def overrideModules: Seq[GuiceableModule] = Seq(
    bind[IdGenerator].toInstance(idGenerator)
  )

  private val hipConnector = app.injector.instanceOf[HipConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()

    Mockito.reset(auditConnector)
    Mockito.reset(idGenerator)
    when(auditConnector.sendEvent(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(AuditResult.Success))
    when(idGenerator.generateUuid).thenReturn(correlationId)
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val correlationId: UUID = UUID.randomUUID()
  private val testNino: String    = TestUtils.randomNino

  private val readExistingProtectionsResponseJson =
    Json.parse(s"""{
                  |  "pensionSchemeAdministratorCheckReference": "PSA12345678A"
                  |}""".stripMargin)

  "HipConnector on amendProtection" must {

    val url =
      s"/lifetime-allowance/person/$testNino/reference/$lifetimeAllowanceIdentifier/sequence-number/$lifetimeAllowanceSequenceNumber"

    val amendProtectionResponseJson =
      Json.parse(s"""{
                    |  "updatedLifetimeAllowanceProtectionRecord": {
                    |    "identifier": $lifetimeAllowanceIdentifier,
                    |    "sequenceNumber": ${lifetimeAllowanceSequenceNumber + 1},
                    |    "type": "${AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta.value}",
                    |    "certificateDate": "2025-07-15",
                    |    "certificateTime": "174312",
                    |    "status": "${AmendProtectionResponseStatus.Open.value}",
                    |    "protectionReference": "$protectionReference",
                    |    "relevantAmount": 105000,
                    |    "preADayPensionInPaymentAmount": 1500,
                    |    "postADayBenefitCrystallisationEventAmount": 2500,
                    |    "uncrystallisedRightsAmount": 75500,
                    |    "nonUKRightsAmount": 0,
                    |    "pensionDebitAmount": 25000,
                    |    "pensionDebitEnteredAmount": 25000,
                    |    "notificationIdentifier": 3,
                    |    "protectedAmount": 120000,
                    |    "pensionDebitStartDate": "2026-07-09",
                    |    "pensionDebitTotalAmount": 40000
                    |  }
                    |}""".stripMargin)

    "call correct HIP endpoint" in {
      stubPost(
        url = url,
        status = OK,
        responseBody = amendProtectionResponseJson.toString
      )

      hipConnector
        .amendProtection(
          nationalInsuranceNumber = testNino,
          lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
          lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
          request = hipAmendProtectionRequest
        )
        .futureValue

      verify(
        postRequestedFor(urlEqualTo(url))
          .withHeader("gov-uk-originator-id", equalTo("test-gov-uk-originator-id"))
          .withHeader("correlationId", equalTo(correlationId.toString))
          .withRequestBody(equalTo(Json.toJson(hipAmendProtectionRequest).toString))
      )
    }

    "return response from HIP" when {

      "HIP returns Ok (200)" in {
        stubPost(
          url = url,
          status = OK,
          responseBody = amendProtectionResponseJson.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        result mustBe Right(hipAmendProtectionResponse)
      }
    }

    "return Left containing an UpstreamErrorResponse" when {

      "HIP returns BadRequest (400)" in {
        val responseBody =
          Json.parse(s"""{
                        |  "origin": "HIP",
                        |  "response": {
                        |     "failures": [
                        |       {
                        |         "reason": "HTTP message not readable",
                        |         "code": "400.2"
                        |       },
                        |       {
                        |         "reason": "Constraint violation - Invalid/Missing input parameter : <parameter>",
                        |         "code": "400.1"
                        |       }
                        |     ]
                        |  }
                        |}""".stripMargin)
        stubPost(
          url = url,
          status = BAD_REQUEST,
          responseBody = responseBody.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val errorResponse = result.swap.getOrElse(UpstreamErrorResponse("msg", 123))

        errorResponse.statusCode mustBe BAD_REQUEST
        errorResponse.message must include(responseBody.toString)
      }

      "HIP returns Forbidden (403)" in {
        val responseBody =
          Json.parse(s"""{
                        |  "code": "403.2",
                        |  "reason": "Forbidden"
                        |}""".stripMargin)
        stubPost(
          url = url,
          status = FORBIDDEN,
          responseBody = responseBody.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val errorResponse = result.swap.getOrElse(UpstreamErrorResponse("msg", 123))

        errorResponse.statusCode mustBe FORBIDDEN
        errorResponse.message must include(responseBody.toString)
      }

      "HIP returns NotFound (404)" in {
        val responseBody =
          Json.parse(s"""{
                        |  "code": "404",
                        |  "reason": "Not Found"
                        |}""".stripMargin)
        stubPost(
          url = url,
          status = NOT_FOUND,
          responseBody = responseBody.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val errorResponse = result.swap.getOrElse(UpstreamErrorResponse("msg", 123))

        errorResponse.statusCode mustBe NOT_FOUND
        errorResponse.message must include(responseBody.toString)
      }

      "HIP returns InternalServerError (500)" in {
        val responseBody =
          Json.parse(s"""{
                        |  "origin": "HIP",
                        |  "response": {
                        |     "failures": [
                        |       {
                        |         "type": "Test Failure Type",
                        |         "reason": "Test Reason"
                        |       }
                        |     ]
                        |  }
                        |}""".stripMargin)
        stubPost(
          url = url,
          status = INTERNAL_SERVER_ERROR,
          responseBody = responseBody.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val errorResponse = result.swap.getOrElse(UpstreamErrorResponse("msg", 123))

        errorResponse.statusCode mustBe INTERNAL_SERVER_ERROR
        errorResponse.message must include(responseBody.toString)
      }

      "HIP returns ServiceUnavailable (503)" in {
        val responseBody =
          Json.parse(s"""{
                        |  "origin": "HIP",
                        |  "response": {
                        |     "failures": [
                        |       {
                        |         "type": "Test Failure Type",
                        |         "reason": "Test Reason"
                        |       }
                        |     ]
                        |  }
                        |}""".stripMargin)
        stubPost(
          url = url,
          status = SERVICE_UNAVAILABLE,
          responseBody = responseBody.toString
        )

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val errorResponse = result.swap.getOrElse(UpstreamErrorResponse("msg", 123))

        errorResponse.statusCode mustBe SERVICE_UNAVAILABLE
        errorResponse.message must include(responseBody.toString)
      }
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

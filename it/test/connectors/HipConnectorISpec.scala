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
import config.HipConfig
import model.hip.existing.ReadExistingProtectionsResponse
import model.hip.{AmendProtectionLifetimeAllowanceType, AmendProtectionResponseStatus}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.MimeTypes
import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import testdata.HipTestData._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, JsValidationException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import util.{IdGenerator, TestUtils}
import utilities.IntegrationSpec

import java.nio.charset.StandardCharsets
import java.util.{Base64, Random, UUID}
import scala.concurrent.Future

class HipConnectorISpec extends IntegrationSpec with EitherValues {

  private val auditConnector = mock[AuditConnector]
  private val idGenerator    = mock[IdGenerator]

  override def overrideModules: Seq[GuiceableModule] = Seq(
    bind[IdGenerator].toInstance(idGenerator)
  )

  private val hipConnector: HipConnector = app.injector.instanceOf[HipConnector]

  private implicit val hipConfig: HipConfig = app.injector.instanceOf[HipConfig]

  private val correlationId: UUID = UUID.randomUUID()

  override def beforeEach(): Unit = {
    super.beforeEach()

    Mockito.reset(auditConnector)
    Mockito.reset(idGenerator)
    when(auditConnector.sendEvent(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(AuditResult.Success))
    when(idGenerator.generateUuid).thenReturn(correlationId)
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val rand          = new Random()
  val ninoGenerator = new Generator(rand)

  val nino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  def token: String =
    Base64.getEncoder
      .encodeToString(
        s"${hipConfig.clientId}:${hipConfig.clientSecret}"
          .getBytes(StandardCharsets.UTF_8)
      )

  private val testNino: String = TestUtils.randomNino

  "HipConnector on amendProtection" must {

    val url =
      s"/paye/lifetime-allowance/person/$testNino/reference/$lifetimeAllowanceIdentifier/sequence-number/$lifetimeAllowanceSequenceNumber"

    val amendProtectionResponseJson =
      Json.parse(s"""{
                    |  "updatedLifetimeAllowanceProtectionRecord": {
                    |    "identifier": $lifetimeAllowanceIdentifier,
                    |    "sequenceNumber": ${lifetimeAllowanceSequenceNumber + 1},
                    |    "type": "${AmendProtectionLifetimeAllowanceType.IndividualProtection2014Lta.toString}",
                    |    "certificateDate": "2025-07-15",
                    |    "certificateTime": "174312",
                    |    "status": "${AmendProtectionResponseStatus.Open.toString}",
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
          .withHeader("gov-uk-originator-id", equalTo("MDTP-LTA-PYLA-2"))
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

    "return failed Future containing JsValidationException when HIP returns incorrect JSON" in {
      val incorrectResponseBody =
        Json.parse(s"""{
                      |  "updatedLifetimeAllowanceProtectionRecord": {
                      |    "identifier": $lifetimeAllowanceIdentifier,
                      |    "sequenceNumber": ${lifetimeAllowanceSequenceNumber + 1},
                      |    "type": "incorrect-type",
                      |    "certificateDate": "2025-07-15",
                      |    "certificateTime": "174312"
                      |  }
                      |}""".stripMargin)
      stubPost(
        url = url,
        status = OK,
        responseBody = incorrectResponseBody.toString
      )

      val result = hipConnector
        .amendProtection(
          nationalInsuranceNumber = testNino,
          lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
          lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
          request = hipAmendProtectionRequest
        )
        .failed
        .futureValue

      result mustBe a[JsValidationException]
    }
  }

  "HipConnector on readExistingProtections" must {

    val url = s"/paye/lifetime-allowance/person/$nino"

    "handle a 200 response from HIP with the correct body" in {

      val responseBody =
        """
          |{
          |  "pensionSchemeAdministratorCheckReference": "PSA34728911G",
          |  "protectionRecordsList": [
          |    {
          |      "protectionRecord": {
          |        "identifier": 20,
          |        "sequenceNumber": 3,
          |        "type": "ENHANCED PROTECTION LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "OPEN",
          |        "protectionReference": "EPRO1034571625B",
          |        "lumpSumPercentage": 12
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 21,
          |          "sequenceNumber": 1,
          |          "type": "ENHANCED PROTECTION",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "REJECTED",
          |          "protectionReference": "EPRO1034571626B",
          |          "lumpSumPercentage": 99
          |        },
          |        {
          |          "identifier": 21,
          |          "sequenceNumber": 1,
          |          "type": "ENHANCED PROTECTION",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "EPRO1034571627B",
          |          "lumpSumPercentage": 99
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 1,
          |        "sequenceNumber": 3,
          |        "type": "PRIMARY PROTECTION LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "PPRO1034571625B",
          |        "pensionDebitAmount": 25000,
          |        "pensionDebitEnteredAmount": 25000,
          |        "pensionDebitStartDate": "2022-07-09",
          |        "pensionDebitTotalAmount": 40000,
          |        "lumpSumAmount": 750000,
          |        "enhancementFactor": 12
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 11,
          |          "sequenceNumber": 1,
          |          "type": "PRIMARY PROTECTION",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "REJECTED",
          |          "protectionReference": "PPRO1034571625B",
          |          "pensionDebitAmount": 25000,
          |          "pensionDebitEnteredAmount": 25000,
          |          "pensionDebitStartDate": "2022-07-09",
          |          "pensionDebitTotalAmount": 40000,
          |          "lumpSumAmount": 750000,
          |          "enhancementFactor": 12
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 2,
          |        "sequenceNumber": 3,
          |        "type": "FIXED PROTECTION LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "FP121034571625B"
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 12,
          |          "sequenceNumber": 3,
          |          "type": "FIXED PROTECTION",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "FP121034571625B"
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 3,
          |        "sequenceNumber": 3,
          |        "type": "FIXED PROTECTION 2014 LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "FP141034571625B"
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 13,
          |          "sequenceNumber": 3,
          |          "type": "FIXED PROTECTION 2014",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "FP141034571625B"
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 4,
          |        "sequenceNumber": 3,
          |        "type": "INDIVIDUAL PROTECTION 2014 LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "DORMANT",
          |        "relevantAmount": 105000,
          |        "preADayPensionInPaymentAmount": 1500,
          |        "postADayBenefitCrystallisationEventAmount": 2500,
          |        "uncrystallisedRightsAmount": 75500,
          |        "nonUKRightsAmount": 0,
          |        "pensionDebitAmount": 25000,
          |        "pensionDebitEnteredAmount": 25000,
          |        "protectedAmount": 120000,
          |        "pensionDebitStartDate": "2022-07-09",
          |        "pensionDebitTotalAmount": 40000
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 14,
          |          "sequenceNumber": 3,
          |          "type": "INDIVIDUAL PROTECTION 2014",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "IP141034571625B",
          |          "relevantAmount": 105000,
          |          "preADayPensionInPaymentAmount": 1500,
          |          "postADayBenefitCrystallisationEventAmount": 2500,
          |          "uncrystallisedRightsAmount": 75500,
          |          "nonUKRightsAmount": 0,
          |          "pensionDebitAmount": 25000,
          |          "pensionDebitEnteredAmount": 25000,
          |          "protectedAmount": 120000,
          |          "pensionDebitStartDate": "2022-07-09",
          |          "pensionDebitTotalAmount": 40000
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 5,
          |        "sequenceNumber": 3,
          |        "type": "FIXED PROTECTION 2016 LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "FP161034571625B"
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 15,
          |          "sequenceNumber": 3,
          |          "type": "FIXED PROTECTION 2016",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "FP161034571625B"
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 6,
          |        "sequenceNumber": 3,
          |        "type": "INDIVIDUAL PROTECTION 2016 LTA",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "DORMANT",
          |        "relevantAmount": 105000,
          |        "preADayPensionInPaymentAmount": 1500,
          |        "postADayBenefitCrystallisationEventAmount": 2500,
          |        "uncrystallisedRightsAmount": 75500,
          |        "nonUKRightsAmount": 0,
          |        "pensionDebitAmount": 25000,
          |        "pensionDebitEnteredAmount": 25000,
          |        "protectedAmount": 120000,
          |        "pensionDebitStartDate": "2022-07-09",
          |        "pensionDebitTotalAmount": 40000
          |      },
          |      "historicaldetailsList": [
          |        {
          |          "identifier": 16,
          |          "sequenceNumber": 3,
          |          "type": "INDIVIDUAL PROTECTION 2016",
          |          "certificateDate": "2021-02-19",
          |          "certificateTime": "091732",
          |          "status": "WITHDRAWN",
          |          "protectionReference": "IP161034571625B",
          |          "relevantAmount": 105000,
          |          "preADayPensionInPaymentAmount": 1500,
          |          "postADayBenefitCrystallisationEventAmount": 2500,
          |          "uncrystallisedRightsAmount": 75500,
          |          "nonUKRightsAmount": 0,
          |          "pensionDebitAmount": 25000,
          |          "pensionDebitEnteredAmount": 25000,
          |          "protectedAmount": 120000,
          |          "pensionDebitStartDate": "2022-07-09",
          |          "pensionDebitTotalAmount": 40000
          |        }
          |      ]
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 7,
          |        "sequenceNumber": 1,
          |        "type": "INTERNATIONAL ENHANCEMENT (S221)",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "IE211034571625B",
          |        "enhancementFactor": 12
          |      }
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 8,
          |        "sequenceNumber": 1,
          |        "type": "INTERNATIONAL ENHANCEMENT (S224)",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "IE241034571625B",
          |        "enhancementFactor": 12
          |      }
          |    },
          |    {
          |      "protectionRecord": {
          |        "identifier": 9,
          |        "sequenceNumber": 1,
          |        "type": "PENSION CREDIT RIGHTS",
          |        "certificateDate": "2021-02-19",
          |        "certificateTime": "091732",
          |        "status": "WITHDRAWN",
          |        "protectionReference": "PCRD1034571625B",
          |        "enhancementFactor": 12
          |      }
          |    }
          |  ]
          |}""".stripMargin

      stubGet(
        url,
        OK,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue

      result mustBe Right(Json.parse(responseBody).as[ReadExistingProtectionsResponse])

      verify(
        getRequestedFor(urlEqualTo(url))
          .withHeader(play.api.http.HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
          .withHeader("Gov-Uk-Originator-Id", equalTo(hipConfig.originatorId))
          .withHeader(HeaderNames.xSessionId, equalTo(hc.sessionId.fold("-")(_.value)))
          .withHeader(HeaderNames.xRequestId, equalTo(hc.requestId.fold("-")(_.value)))
          .withHeader("CorrelationId", equalTo(correlationId.toString))
          .withHeader(HeaderNames.authorisation, equalTo(s"Basic $token"))
      )
    }

    "handle missing protectionRecordList in response from HIP" in {

      val responseBody =
        """
          |{
          |  "pensionSchemeAdministratorCheckReference": "PSA34728911G"
          |}""".stripMargin

      stubGet(
        url,
        OK,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue

      result mustBe Right(Json.parse(responseBody).as[ReadExistingProtectionsResponse])
    }

    "handle a 400 response" in {

      val responseBody = """
                           |{
                           |  "origin": "HoD",
                           |  "response": {
                           |    "failures": [
                           |      {
                           |        "reason": "HTTP message not readable",
                           |        "code": "400.2"
                           |      },
                           |      {
                           |        "reason": "Constraint violation - Invalid/Missing input parameter : <parameter>",
                           |        "code": "400.1"
                           |      }
                           |    ]
                           |  }
                           |}""".stripMargin

      stubGet(
        url,
        BAD_REQUEST,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue.left.value

      result.message must include(responseBody)
      result.statusCode mustBe BAD_REQUEST
    }

    "handle a 403 response" in {

      val responseBody =
        """
          |{
          |  "reason": "Forbidden",
          |  "code": "403.2"
          |}
          |""".stripMargin

      stubGet(
        url,
        FORBIDDEN,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue.left.value

      result.message must include(responseBody)
      result.statusCode mustBe FORBIDDEN
    }

    "handle a 500 response" in {

      val responseBody =
        """
          |{
          |  "origin": "HIP",
          |  "response": {
          |    "failures": [
          |      {
          |        "type": "string",
          |        "reason": "string"
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin

      stubGet(
        url,
        INTERNAL_SERVER_ERROR,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue.left.value

      result.message must include(responseBody)
      result.statusCode mustBe INTERNAL_SERVER_ERROR
    }

    "handle a 503 response" in {

      val responseBody =
        """
          |{
          |  "origin": "HIP",
          |  "response": {
          |    "failures": [
          |      {
          |        "type": "string",
          |        "reason": "string"
          |      }
          |    ]
          |  }
          |}
          |""".stripMargin

      stubGet(
        url,
        SERVICE_UNAVAILABLE,
        responseBody
      )

      val result = hipConnector.readExistingProtections(nino).futureValue.left.value

      result.message must include(responseBody)
      result.statusCode mustBe SERVICE_UNAVAILABLE
    }
  }

}

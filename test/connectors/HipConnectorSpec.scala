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

import config.HipConfig
import events.HipAmendLtaEvent
import model.hip.HipAmendProtectionResponse
import model.hip.existing.{ProtectionRecordsList, ReadExistingProtectionsResponse}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import testdata.HipTestData._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, NotFoundException, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import util.IdGenerator
import util.TestUtils.testNino

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HipConnectorSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with ScalaFutures {

  private val hipConfig      = mock[HipConfig]
  private val idGenerator    = mock[IdGenerator]
  private val httpClient     = mock[HttpClientV2]
  private val auditConnector = mock[AuditConnector]
  private val requestBuilder = mock[RequestBuilder]

  private val hipConnector = new HipConnector(hipConfig, idGenerator, httpClient, auditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(hipConfig)
    reset(idGenerator)
    reset(httpClient)
    reset(auditConnector)
    reset(requestBuilder)

    when(hipConfig.baseUrl).thenReturn(urlBase)
    when(hipConfig.originatorId).thenReturn(originatorId)
    when(hipConfig.clientId).thenReturn(clientId)
    when(hipConfig.clientSecret).thenReturn(clientSecret)
    when(httpClient.post(any())(any())).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val correlationId = UUID.randomUUID()

  private val urlBase      = "http://localhost:12345/hip"
  private val originatorId = "test-originator-id"
  private val clientId     = "test-client-id"
  private val clientSecret = "test-client-secret"

  "HipConnector on amendProtection" when {

    val requestUrl =
      urlBase + s"/lifetime-allowance/person/$testNino/reference/$lifetimeAllowanceIdentifier/sequence-number/$lifetimeAllowanceSequenceNumber"

    "everything works correctly" should {

      "call AuditConnector" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HipAmendProtectionResponse]](any(), any()))
          .thenReturn(Future.successful(Right(hipAmendProtectionResponse)))
        when(auditConnector.sendEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
        verify(auditConnector).sendEvent(captor.capture())(any(), any())

        val actualDataEvent = captor.getValue
        val expectedAuditEvent = new HipAmendLtaEvent(
          nino = testNino,
          id = lifetimeAllowanceIdentifier,
          hipRequestBodyJs = Json.toJson(hipAmendProtectionRequest).as[JsObject],
          hipResponseBodyJs = Json.toJson(hipAmendProtectionResponse).as[JsObject],
          statusCode = OK,
          path = requestUrl
        ).copy(eventId = actualDataEvent.eventId, generatedAt = actualDataEvent.generatedAt)

        actualDataEvent shouldBe expectedAuditEvent
      }

      "return the value obtained from HttpClientV2.execute" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        when(requestBuilder.execute[Either[UpstreamErrorResponse, HipAmendProtectionResponse]](any(), any()))
          .thenReturn(Future.successful(Right(hipAmendProtectionResponse)))
        when(auditConnector.sendEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .futureValue

        result shouldBe Right(hipAmendProtectionResponse)
      }
    }

    "HttpClientV2 returns BadRequestException" should {

      "NOT call AuditConnector" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new BadRequestException("Test error message")
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        verifyNoInteractions(auditConnector)
      }

      "return this exception" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new BadRequestException("Test error message")
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        result shouldBe testException
      }
    }

    "HttpClientV2 returns NotFoundException" should {

      "NOT call AuditConnector" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new NotFoundException("Test error message")
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        verifyNoInteractions(auditConnector)
      }

      "return this exception" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new NotFoundException("Test error message")
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        result shouldBe testException
      }
    }

    "HttpClientV2 returns UpstreamErrorResponse" should {

      "NOT call AuditConnector" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new UpstreamErrorResponse("Test error message", 500, 500, Map.empty)
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        verifyNoInteractions(auditConnector)
      }

      "return this exception" in {
        when(idGenerator.generateUuid).thenReturn(correlationId)
        val testException = new UpstreamErrorResponse("Test error message", 500, 500, Map.empty)
        when(requestBuilder.execute(any(), any())).thenReturn(Future.failed(testException))

        val result = hipConnector
          .amendProtection(
            nationalInsuranceNumber = testNino,
            lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
            lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber,
            request = hipAmendProtectionRequest
          )
          .failed
          .futureValue

        result shouldBe testException
      }
    }
  }

  "HipConnector on padCertificateTime" should {
    "pad with leading zeros up to a length of 6" when {
      "certificateTime is 1" in {
        hipConnector.padCertificateTime("1") shouldBe "000001"
      }

      "certificateTime is 11" in {
        hipConnector.padCertificateTime("11") shouldBe "000011"
      }

      "certificateTime is 111" in {
        hipConnector.padCertificateTime("111") shouldBe "000111"
      }

      "certificateTime is 1111" in {
        hipConnector.padCertificateTime("1111") shouldBe "001111"
      }

      "certificateTime is 11111" in {
        hipConnector.padCertificateTime("11111") shouldBe "011111"
      }

      "certificateTime is 111111" in {
        hipConnector.padCertificateTime("111111") shouldBe "111111"
      }
    }
  }

  "HipConnector on padCertificateTimeInReadExistingProtectionsResponse" should {
    "pad certificate time with leading zeros up to a length of 6" in {
      val unpaddedProtectionRecord =
        hipReadExistingProtectionsResponse.protectionRecordsList.get.head.protectionRecord.copy(
          certificateTime = "93010"
        )

      val unpaddedReadExistingProtectionsResponse = ReadExistingProtectionsResponse(
        pensionSchemeAdministratorCheckReference,
        Some(
          Seq(
            ProtectionRecordsList(
              unpaddedProtectionRecord,
              Some(List(unpaddedProtectionRecord))
            )
          )
        )
      )

      val paddedProtectionRecord = unpaddedProtectionRecord.copy(
        certificateTime = "093010"
      )

      val paddedReadExistingProtectionsResponse = ReadExistingProtectionsResponse(
        pensionSchemeAdministratorCheckReference,
        Some(
          Seq(
            ProtectionRecordsList(
              paddedProtectionRecord,
              Some(List(paddedProtectionRecord))
            )
          )
        )
      )

      hipConnector.padCertificateTime(
        unpaddedReadExistingProtectionsResponse
      ) shouldBe paddedReadExistingProtectionsResponse
    }

  }

  "HipController on padCertificateTimeInHipAmendProtectionResponse" should {
    "pad certificate time with leading zeros up to a length of 6" in {
      val updatedLifetimeAllowanceProtectionRecord = hipAmendProtectionResponse.updatedLifetimeAllowanceProtectionRecord

      val unpaddedHipAmendProtectionResponse = HipAmendProtectionResponse(
        updatedLifetimeAllowanceProtectionRecord.copy(
          certificateTime = Some("93010")
        )
      )

      val paddedHipAmendProtectionResponse = HipAmendProtectionResponse(
        updatedLifetimeAllowanceProtectionRecord.copy(
          certificateTime = Some("093010")
        )
      )

      hipConnector.padCertificateTime(
        unpaddedHipAmendProtectionResponse
      ) shouldBe paddedHipAmendProtectionResponse
    }
  }

}

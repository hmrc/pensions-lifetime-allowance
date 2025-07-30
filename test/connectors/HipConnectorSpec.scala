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

import events.HipAmendLtaEvent
import model.hip.HipAmendProtectionResponse
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
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import util.IdGenerator
import util.TestUtils.testNino

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HipConnectorSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with ScalaFutures {

  private val idGenerator    = mock[IdGenerator]
  private val httpClient     = mock[HttpClientV2]
  private val servicesConfig = mock[ServicesConfig]
  private val auditConnector = mock[AuditConnector]
  private val requestBuilder = mock[RequestBuilder]

  private val hipConnector = new HipConnector(idGenerator, httpClient, servicesConfig, auditConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(idGenerator, httpClient, servicesConfig, auditConnector, requestBuilder)

    when(servicesConfig.baseUrl(any())).thenReturn("http://localhost:12345/hip")
    when(httpClient.post(any())(any())).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any())(any(), any(), any())).thenReturn(requestBuilder)
    when(requestBuilder.setHeader(any())).thenReturn(requestBuilder)
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val correlationId = UUID.randomUUID()

  "HipConnector on amendProtection" when {

    val urlBase = "http://localhost:12345/hip"
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

}

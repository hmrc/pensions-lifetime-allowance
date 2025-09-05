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
import model.hip.existing.ReadExistingProtectionsResponse
import model.hip.{HipAmendProtectionRequest, HipAmendProtectionResponse}
import play.api.Logging
import play.api.http.MimeTypes
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import util.IdGenerator

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (
    hipConfig: HipConfig,
    idGenerator: IdGenerator,
    httpClient: HttpClientV2,
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  private def baseUrl: String = hipConfig.baseUrl

  private def amendProtectionUrl(
      nationalInsuranceNumber: String,
      lifetimeAllowanceIdentifier: Int,
      lifetimeAllowanceSequenceNumber: Int
  ): String =
    baseUrl + s"/lifetime-allowance/person/$nationalInsuranceNumber/reference/$lifetimeAllowanceIdentifier/sequence-number/$lifetimeAllowanceSequenceNumber"

  private def readExistingProtectionsUrl(nino: String): String = hipConfig.baseUrl + s"/lifetime-allowance/person/$nino"

  private def basicHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val token =
      Base64.getEncoder
        .encodeToString(
          s"${hipConfig.clientId}:${hipConfig.clientSecret}"
            .getBytes(StandardCharsets.UTF_8)
        )

    Seq(
      play.api.http.HeaderNames.CONTENT_TYPE -> MimeTypes.JSON,
      "Gov-Uk-Originator-Id"                 -> hipConfig.originatorId,
      HeaderNames.xSessionId                 -> hc.sessionId.fold("-")(_.value),
      HeaderNames.xRequestId                 -> hc.requestId.fold("-")(_.value),
      "CorrelationId"                        -> idGenerator.generateUuid.toString,
      HeaderNames.authorisation              -> s"Basic $token"
    )
  }

  def amendProtection(
      nationalInsuranceNumber: String,
      lifetimeAllowanceIdentifier: Int,
      lifetimeAllowanceSequenceNumber: Int,
      request: HipAmendProtectionRequest
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, HipAmendProtectionResponse]] = {

    val urlString = amendProtectionUrl(
      nationalInsuranceNumber,
      lifetimeAllowanceIdentifier = lifetimeAllowanceIdentifier,
      lifetimeAllowanceSequenceNumber = lifetimeAllowanceSequenceNumber
    )

    for {
      amendProtectionResponseE <- httpClient
        .post(url"$urlString")
        .withBody(Json.toJson(request))
        .setHeader(basicHeaders: _*)
        .execute[HttpResponse]
        .map { httpResponse =>
          val responseStatus = httpResponse.status
          val responseBody   = httpResponse.json

          logger.info(
            s"Called HIP API POST $urlString endpoint with NINo: $nationalInsuranceNumber. Response status: $responseStatus, body: $responseBody"
          )

          readEitherOf[HipAmendProtectionResponse].read("POST", urlString, httpResponse)
        }

      _ = amendProtectionResponseE.map { amendProtectionResponse =>
        sendAuditEvent(
          nino = nationalInsuranceNumber,
          id = lifetimeAllowanceIdentifier,
          requestUrl = urlString,
          requestBody = request,
          responseStatusCode = OK,
          responseBody = amendProtectionResponse
        )
      }

    } yield amendProtectionResponseE
  }

  private def sendAuditEvent(
      nino: String,
      id: Int,
      requestUrl: String,
      requestBody: HipAmendProtectionRequest,
      responseStatusCode: Int,
      responseBody: HipAmendProtectionResponse
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {

    val auditEvent = new HipAmendLtaEvent(
      nino = nino,
      id = id,
      hipRequestBodyJs = Json.toJson(requestBody).as[JsObject],
      hipResponseBodyJs = Json.toJson(responseBody).as[JsObject],
      statusCode = responseStatusCode,
      path = requestUrl
    )

    logger.debug(s"Sending audit event: $auditEvent")
    auditConnector.sendEvent(auditEvent)
  }

  def readExistingProtections(
      nino: String
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ReadExistingProtectionsResponse]] = {
    val urlString = readExistingProtectionsUrl(nino)

    httpClient
      .get(url"$urlString")
      .setHeader(basicHeaders: _*)
      .execute[HttpResponse]
      .map { httpResponse =>
        val responseStatus = httpResponse.status
        val responseBody   = httpResponse.json

        logger.info(
          s"Called HIP API GET $urlString endpoint with NINo: $nino. Response status: $responseStatus, body: $responseBody"
        )

        readEitherOf[ReadExistingProtectionsResponse].read("GET", urlString, httpResponse)
      }
  }

}

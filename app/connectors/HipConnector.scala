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
import model.hip.AmendProtectionResponse
import model.hip.existing.ReadExistingProtectionsResponse
import play.api.Logging
import play.api.http.MimeTypes
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import util.IdGenerator

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (
    httpClient: HttpClientV2,
    hipConfig: HipConfig,
    idGenerator: IdGenerator,
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  private def amendProtectionUrl: String                       = hipConfig.baseUrl + "/amend"
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

  def amendProtection()(implicit hc: HeaderCarrier): Future[AmendProtectionResponse] =
    httpClient.post(url"$amendProtectionUrl").execute[AmendProtectionResponse]

  def readExistingProtections(
      nino: String
  )(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ReadExistingProtectionsResponse]] =
    httpClient
      .get(url"${readExistingProtectionsUrl(nino)}")
      .setHeader(basicHeaders: _*)
      .execute[Either[UpstreamErrorResponse, ReadExistingProtectionsResponse]]

}

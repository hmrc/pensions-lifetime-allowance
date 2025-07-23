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

import model.hip.{AmendProtectionResponse, ReadExistingProtectionsResponse}
import play.api.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig,
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  private def baseUrl: String = servicesConfig.baseUrl("hip")

  private def amendProtectionUrl: String         = baseUrl + "/amend"
  private def readExistingProtectionsUrl: String = baseUrl + "/read"

  def amendProtection()(implicit hc: HeaderCarrier): Future[AmendProtectionResponse] =
    httpClient.post(url"$amendProtectionUrl").execute[AmendProtectionResponse]

  def readExistingProtections()(implicit hc: HeaderCarrier): Future[ReadExistingProtectionsResponse] =
    httpClient.get(url"$readExistingProtectionsUrl").execute[ReadExistingProtectionsResponse]

}

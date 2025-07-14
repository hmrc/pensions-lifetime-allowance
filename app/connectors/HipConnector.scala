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

import model.HttpResponseDetails
import play.api.Logging
import play.api.libs.json.{JsObject, JsSuccess}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.Future

class HipConnector @Inject() (
    httpClient: HttpClientV2,
    servicesConfig: ServicesConfig,
    auditConnector: AuditConnector
) extends Logging {

  def amendProtection(): Future[HttpResponseDetails] =
    Future.successful(HttpResponseDetails(200, JsSuccess(JsObject.empty)))

  def readExistingProtections(): Future[HttpResponseDetails] =
    Future.successful(HttpResponseDetails(200, JsSuccess(JsObject.empty)))

}

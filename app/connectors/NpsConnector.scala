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

package connectors

import play.api.{Environment, Logging, Mode}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultNpsConnector @Inject() (
    val http: HttpClientV2,
    environment: Environment,
    servicesConfig: ServicesConfig,
    val audit: AuditConnector
) extends NpsConnector {
  override lazy val serviceUrl: String         = servicesConfig.baseUrl("nps")
  override lazy val serviceAccessToken: String = servicesConfig.getConfString("nps.accessToken", "")
  override lazy val serviceEnvironment: String = servicesConfig.getConfString("nps.environment", "")

  val mode: Mode = environment.mode
}

trait NpsConnector extends Logging {
  val http: HttpClientV2
  val serviceUrl: String
  val serviceAccessToken: String
  val serviceEnvironment: String
  val audit: AuditConnector

  def httpHeaders(): Seq[(String, String)] = Seq(
    "Accept"        -> "application/vnd.hmrc.1.0+json",
    "Content-Type"  -> "application/json",
    "Environment"   -> serviceEnvironment,
    "Authorization" -> s"Bearer $serviceAccessToken"
  )

  def getPSALookup(
      psaRef: String,
      ltaRef: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val requestUrl =
      s"$serviceUrl/pensions-lifetime-allowance/scheme-administrator/certificate-lookup?pensionSchemeAdministratorCheckReference=$psaRef&lifetimeAllowanceReference=$ltaRef"
    get(requestUrl)(hc, ec).map(r => r)
  }

  def get(requestUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    http.get(url"$requestUrl").setHeader(httpHeaders(): _*).execute[HttpResponse]

}

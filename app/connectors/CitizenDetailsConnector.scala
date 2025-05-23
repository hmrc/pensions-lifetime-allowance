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

import play.api.{Environment, Mode}
import play.api.http.Status._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultCitizenDetailsConnector @Inject() (
    val http: HttpClientV2,
    environment: Environment,
    servicesConfig: ServicesConfig
) extends CitizenDetailsConnector {

  override lazy val serviceUrl: String     = servicesConfig.baseUrl("citizen-details")
  override lazy val checkRequired: Boolean = servicesConfig.getConfBool("citizen-details.checkRequired", defBool = true)

  val mode: Mode = environment.mode
}

sealed trait CitizenRecordCheckResult

case object CitizenRecordOK extends CitizenRecordCheckResult

case object CitizenRecordLocked extends CitizenRecordCheckResult

case object CitizenRecordNotFound extends CitizenRecordCheckResult

case class CitizenRecordOther4xxResponse(e: UpstreamErrorResponse) extends CitizenRecordCheckResult

case class CitizenRecord5xxResponse(e: UpstreamErrorResponse) extends CitizenRecordCheckResult

trait CitizenDetailsConnector {
  def http: HttpClientV2

  val serviceUrl: String
  val checkRequired: Boolean

  def getCitizenRecordCheckUrl(nino: String): String =
    serviceUrl + s"/citizen-details/$nino/designatory-details"

  implicit val legacyRawReads: HttpReads[HttpResponse] =
    HttpReadsInstances.throwOnFailure(HttpReadsInstances.readEitherOf(HttpReadsInstances.readRaw))

  def checkCitizenRecord(
      nino: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CitizenRecordCheckResult] =
    if (!checkRequired) {
      Future.successful(CitizenRecordOK)
    } else {
      val requestUrl = getCitizenRecordCheckUrl(nino)
      http.get(url"$requestUrl").execute[HttpResponse].map(_ => CitizenRecordOK).recover {
        case e: UpstreamErrorResponse =>
          e.statusCode match {
            case NOT_FOUND => CitizenRecordNotFound
            case LOCKED    => CitizenRecordLocked
            case status if status >= BAD_REQUEST && status < INTERNAL_SERVER_ERROR =>
              CitizenRecordOther4xxResponse(e)
            case status if status >= INTERNAL_SERVER_ERROR => CitizenRecord5xxResponse(e)
          }
      }
    }

}

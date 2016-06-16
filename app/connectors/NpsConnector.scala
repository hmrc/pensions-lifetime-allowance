/*
 * Copyright 2016 HM Revenue & Customs
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

import util.NinoHelper
import config.WSHttp
import config.MicroserviceAuditConnector

import events.NPSCreateLTAEvent
import play.api.libs.json._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, _}
import model.HttpResponseDetails
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}

object NpsConnector extends NpsConnector with ServicesConfig {

  override val serviceUrl = baseUrl("nps")
  override def http = WSHttp

  override val audit = MicroserviceAuditConnector

  override val serviceAccessToken = getConfString("nps.accessToken", "")
  override val serviceEnvironment = getConfString("nps.environment", "")


}
trait NpsConnector {

  def http: HttpGet with HttpPost with HttpPut
  val serviceUrl: String

  val serviceAccessToken: String
  val serviceEnvironment: String

  val audit: AuditConnector

  // add addtional headers for the NPS request
  def addExtraHeaders(implicit hc: HeaderCarrier): HeaderCarrier = hc.withExtraHeaders(
    "Accept" -> "application/vnd.hmrc.1.0+json",
    "Content-Type" -> "application/json",
    "Authorization" -> s"Bearer $serviceAccessToken",
    "Environment" -> serviceEnvironment)

  def getUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/${ninoWithoutSuffix}/protection"
  }

  implicit val readApiResponse: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = NpsResponseHandler.handleNpsResponse(method, url, response)
  }

  def applyForProtection(nino: String, body: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl = getUrl(nino)
    val requestTime = DateTimeUtils.now

    val responseFut = post(requestUrl, body)(hc = addExtraHeaders(hc), ec = ec)

    responseFut map { expectedResponse =>
      handleExpectedApplyResponse(requestUrl,nino,requestTime, body,expectedResponse)
    }
  }

  def handleExpectedApplyResponse(
      requestUrl: String,
      nino: String,
      requestTime: org.joda.time.DateTime,
      requestBody: JsObject,
      response: HttpResponse)(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponseDetails = {

    val createLTAEvent = NPSCreateLTAEvent(nino, requestUrl, requestTime, requestBody, response.json.as[JsObject], response.status)
    audit.sendMergedEvent(createLTAEvent)
    HttpResponseDetails(response.status, JsSuccess(response.json.as[JsObject]))
  }

  def post(requestUrl: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    http.POST[JsValue, HttpResponse](requestUrl, body)
  }
}

object NpsResponseHandler extends NpsResponseHandler

trait NpsResponseHandler extends HttpErrorFunctions {
  def handleNpsResponse(method: String, url: String, response: HttpResponse): HttpResponse = {
    response.status match {
      case 409 => response // this is an expected response for this API, so don't throw an exception
      case _ => handleResponse(method, url)(response)
    }
  }
}
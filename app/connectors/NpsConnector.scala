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

import events.{NPSAmendLTAEvent, NPSBaseLTAEvent, NPSCreateLTAEvent}
import model.{Error, HttpResponseDetails}
import play.api.{Environment, Logging, Mode}
import util.NinoHelper
import play.api.libs.json._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpErrorFunctions,
  HttpReads,
  HttpResponse,
  NotFoundException,
  StringContextOps
}
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

  def getAmendUrl(nino: String, id: Long): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"
  }

  def getReadUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections"
  }

  implicit val readApiResponse: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse): HttpResponse =
      NpsResponseHandler.handleNpsResponse(method, url, response)
  }

  def amendProtection(nino: String, id: Long, body: JsObject)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[HttpResponseDetails] = {
    val requestUrl  = getAmendUrl(nino, id)
    val responseFut = put(requestUrl, body)

    responseFut.map { response =>
      val auditEvent = new NPSAmendLTAEvent(
        nino = nino,
        id = id,
        npsRequestBodyJs = body,
        npsResponseBodyJs = response.json.as[JsObject],
        statusCode = response.status,
        path = requestUrl
      )
      handleAuditableResponse(nino, response, Some(auditEvent))
    }
  }

  def getPSALookup(
      psaRef: String,
      ltaRef: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val requestUrl =
      s"$serviceUrl/pensions-lifetime-allowance/scheme-administrator/certificate-lookup?pensionSchemeAdministratorCheckReference=$psaRef&lifetimeAllowanceReference=$ltaRef"
    get(requestUrl)(hc, ec).map(r => r)
  }

  def handleAuditableResponse(nino: String, response: HttpResponse, auditEvent: Option[NPSBaseLTAEvent])(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext
  ): HttpResponseDetails = {
    val responseBody = response.json.as[JsObject]
    val httpStatus   = response.status

    logger.debug(s"Created audit event: ${auditEvent.getOrElse("<None>")}")
    auditEvent.foreach {
      audit.sendEvent(_)
    }

    // assertion: nino returned in response must be the same as that sent in the request
    val responseNino           = responseBody.value.get("nino").map(n => n.as[String]).getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino == ninoWithoutSuffix) {
      HttpResponseDetails(httpStatus, JsSuccess(responseBody))
    } else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      logger.warn(report)
      HttpResponseDetails(400, JsSuccess(Json.toJson(Error(report)).as[JsObject]))
    }
  }

  def put(requestUrl: String, body: JsValue)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    http.put(url"$requestUrl").withBody(Json.toJson(body)).setHeader(httpHeaders(): _*).execute[HttpResponse]

  def readExistingProtections(
      nino: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val requestUrl  = getReadUrl(nino)
    val responseFut = get(requestUrl)(hc: HeaderCarrier, ec = ec)

    responseFut.map(expectedResponse => handleExpectedReadResponse(nino, expectedResponse))
  }

  def get(requestUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    http.get(url"$requestUrl").setHeader(httpHeaders(): _*).execute[HttpResponse]

  def handleExpectedReadResponse(nino: String, response: HttpResponse): HttpResponseDetails = {

    val responseBody           = response.json.as[JsObject]
    val responseNino           = responseBody.value.get("nino").map(n => n.as[String]).getOrElse("")
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    if (responseNino == ninoWithoutSuffix) {
      HttpResponseDetails(response.status, JsSuccess(responseBody))
    } else {
      val report = s"Received nino $responseNino is not same as sent nino $ninoWithoutSuffix"
      logger.warn(report)
      HttpResponseDetails(400, JsSuccess(Json.toJson(Error(report)).as[JsObject]))
    }
  }

}

object NpsResponseHandler extends NpsResponseHandler

trait NpsResponseHandler extends HttpErrorFunctions {

  /** Response handler for NSP type responses. Note: Expected to throw exception 409
    */
  def handleNpsResponse(method: String, url: String, response: HttpResponse): HttpResponse =
    response.status match {
      case 409 => response // this is an expected response for this API, so don't throw an exception
      case _ =>
        response.status match {
          case status if is4xx(status) => throw new NotFoundException(notFoundMessage(method, url, response.body))
          case _                       => response
        }
    }

}

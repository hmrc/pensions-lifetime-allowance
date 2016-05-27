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

import config.WSHttp
import play.api.libs.json.{JsResult, JsObject, JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future

object NpsConnector extends NpsConnector with ServicesConfig {

  override val serviceUrl = baseUrl("pensions-lifetime-allowance")
  override def http = WSHttp
}
trait NpsConnector {

  // add addtional headers
  implicit val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Content-Type" -> "application/json")
  val http: HttpGet with HttpPost with HttpPut
  val serviceUrl: String
  def url(path: String): String = s"$serviceUrl$path"
  private def ninoWithoutSuffix(nino: String): String = nino.substring(0, 8)

  def applyForProtection(nino: String, body: JsObject)(implicit hc: HeaderCarrier): Future[JsObject] = {
    val requestUrl = url(  s"/individual/${ninoWithoutSuffix(nino)}/protection")
//    val requestJson: JsValue = Json.parse("""{"protectionType":1}""")

    val responseFut = http.POST[JsValue, HttpResponse](requestUrl, body)
    responseFut.map { response =>
      response.json.as[JsObject]
    }
  }
}

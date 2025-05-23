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

package controllers

import connectors.NpsConnector

import javax.inject.Inject
import model.Error
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class DefaultLookupController @Inject() (val npsConnector: NpsConnector, cc: ControllerComponents)(
    implicit ec: ExecutionContext
) extends BackendController(cc)
    with NPSResponseHandler {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def psaLookup(psaRef: String, ltaRef: String): Action[AnyContent] = Action.async {
    npsConnector
      .getPSALookup(psaRef, ltaRef)
      .map { response =>
        response.status match {
          case OK =>
            Ok(response.json)
          case _ =>
            val error = Json.toJson(
              Error(
                s"NPS request resulted in a response with: HTTP status = ${response.status} body = ${response.json}"
              )
            )
            logger.error(error.toString)
            InternalServerError(error)
        }
      }
      .recover { case error => handleNPSError(error, "[LookupController.psaLookup]") }
  }

}

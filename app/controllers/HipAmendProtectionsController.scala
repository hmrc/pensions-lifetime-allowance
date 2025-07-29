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

package controllers

import auth.{AuthClientConnector, AuthorisedActions}
import connectors.CitizenDetailsConnector
import model.Error
import model.api.AmendProtectionRequest
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.HipProtectionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipAmendProtectionsController @Inject() (
    val authConnector: AuthClientConnector,
    val citizenDetailsConnector: CitizenDetailsConnector,
    hipProtectionService: HipProtectionService,
    cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedActions {

  def amendProtection(nino: String, protectionId: Int): Action[JsValue] =
    Action.async(cc.parsers.json) { implicit request =>
      userAuthorised(nino) {
        request.body
          .validate[AmendProtectionRequest]
          .fold(
            errors =>
              Future
                .successful(BadRequest(Json.toJson(Error(message = "body failed validation with error: " + errors)))),
            amendProtectionRequest =>
              hipProtectionService
                .amendProtection(nino, protectionId, amendProtectionRequest)
                .map {
                  case Right(amendProtectionResponse) => Ok(Json.toJson(amendProtectionResponse))
                  case Left(error) =>
                    logger.warn(s"An error occurred when amending protection: ${error.getMessage}")
                    InternalServerError(error.getMessage)
                }
          )
      }
    }

}

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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.HipProtectionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HipReadProtectionsController @Inject() (
  val authConnector: AuthClientConnector,
  val citizenDetailsConnector: CitizenDetailsConnector,
  hipProtectionService: HipProtectionService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with AuthorisedActions {

  def readExistingProtections(nino: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised(nino) {
      hipProtectionService
        .readExistingProtections()
        .map(readExistingProtectionsResponse => Ok(Json.toJson(readExistingProtectionsResponse)))
    }
  }

}

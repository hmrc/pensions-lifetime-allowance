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

package auth

import javax.inject.Inject
import play.api.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

class DefaultAuthClientConnector @Inject()(val http: DefaultHttpClient,
                                           environment: Environment,
                                           val runModeConfiguration: Configuration,
                                           servicesConfig: ServicesConfig) extends AuthClientConnector {
  val mode: Mode = environment.mode
  override lazy val serviceUrl: String = servicesConfig.baseUrl("auth")
}

trait AuthClientConnector extends PlayAuthConnector
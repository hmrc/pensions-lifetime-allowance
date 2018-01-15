/*
 * Copyright 2018 HM Revenue & Customs
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

package mock

import auth.AuthClientConnectorTrait
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.Future

trait AuthMock extends MockitoSugar{
  this: MockitoSugar =>

  val mockAuthConnector = mock[AuthClientConnectorTrait]

  def mockAuthConnector[T](future: Future[T]): OngoingStubbing[Future[T]] = {
    when(mockAuthConnector.authorise[T](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(future)
  }
}
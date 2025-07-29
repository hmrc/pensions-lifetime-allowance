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

package model.hip

import play.api.libs.json._

sealed trait AmendProtectionResponseStatus {
  val value: String
}

object AmendProtectionResponseStatus {

  case object Open extends AmendProtectionResponseStatus {
    override val value: String = "OPEN"
  }

  case object Dormant extends AmendProtectionResponseStatus {
    override val value: String = "DORMANT"
  }

  case object Withdrawn extends AmendProtectionResponseStatus {
    override val value: String = "WITHDRAWN"
  }

  private val allStatuses: Seq[AmendProtectionResponseStatus] =
    Seq(Open, Dormant, Withdrawn)

  implicit val format: Format[AmendProtectionResponseStatus] = {
    val jsonWrites: Writes[AmendProtectionResponseStatus] = new Writes[AmendProtectionResponseStatus] {
      override def writes(responseStatus: AmendProtectionResponseStatus): JsValue =
        JsString(responseStatus.value)
    }

    val jsonReads: Reads[AmendProtectionResponseStatus] = new Reads[AmendProtectionResponseStatus] {
      override def reads(json: JsValue): JsResult[AmendProtectionResponseStatus] =
        json match {
          case JsString(str) =>
            allStatuses
              .find(_.value == str)
              .fold[JsResult[AmendProtectionResponseStatus]](
                JsError(s"Received unknown AmendProtectionResponseStatus: $str")
              )(
                JsSuccess(_)
              )

          case other =>
            JsError(s"Cannot create AmendProtectionResponseStatus instance from: ${other.toString}")
        }
    }

    Format(jsonReads, jsonWrites)
  }

}

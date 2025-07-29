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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Reads, Writes}

sealed trait AmendProtectionRequestStatus {
  val value: String
}

object AmendProtectionRequestStatus {

  case object Open extends AmendProtectionRequestStatus {
    override val value: String = "OPEN"
  }

  case object Dormant extends AmendProtectionRequestStatus {
    override val value: String = "DORMANT"
  }

  private val allStatuses: Seq[AmendProtectionRequestStatus] =
    Seq(Open, Dormant)

  implicit val format: Format[AmendProtectionRequestStatus] = {
    val jsonWrites: Writes[AmendProtectionRequestStatus] = new Writes[AmendProtectionRequestStatus] {
      override def writes(AmendProtectionRequestStatus: AmendProtectionRequestStatus): JsValue =
        JsString(AmendProtectionRequestStatus.value)
    }

    val jsonReads: Reads[AmendProtectionRequestStatus] = new Reads[AmendProtectionRequestStatus] {
      override def reads(json: JsValue): JsResult[AmendProtectionRequestStatus] =
        json match {
          case JsString(str) =>
            allStatuses
              .find(_.value == str)
              .fold[JsResult[AmendProtectionRequestStatus]](
                JsError(s"Received unknown AmendProtectionRequestStatus: $str")
              )(
                JsSuccess(_)
              )

          case other =>
            JsError(s"Cannot create AmendProtectionRequestStatus instance from: ${other.toString}")
        }
    }

    Format(jsonReads, jsonWrites)
  }

}

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

sealed trait AmendProtectionLifetimeAllowanceType {
  val value: String
}

object AmendProtectionLifetimeAllowanceType {

  case object IndividualProtection2014 extends AmendProtectionLifetimeAllowanceType {
    override val value: String = "INDIVIDUAL PROTECTION 2014"
  }

  case object IndividualProtection2016 extends AmendProtectionLifetimeAllowanceType {
    override val value: String = "INDIVIDUAL PROTECTION 2016"
  }

  case object IndividualProtection2014Lta extends AmendProtectionLifetimeAllowanceType {
    override val value: String = "INDIVIDUAL PROTECTION 2014 LTA"
  }

  case object IndividualProtection2016Lta extends AmendProtectionLifetimeAllowanceType {
    override val value: String = "INDIVIDUAL PROTECTION 2016 LTA"
  }

  private val allTypes: Seq[AmendProtectionLifetimeAllowanceType] = Seq(
    IndividualProtection2014,
    IndividualProtection2016,
    IndividualProtection2014Lta,
    IndividualProtection2016Lta
  )

  implicit val format: Format[AmendProtectionLifetimeAllowanceType] = {
    val jsonWrites: Writes[AmendProtectionLifetimeAllowanceType] = new Writes[AmendProtectionLifetimeAllowanceType] {
      override def writes(ltaType: AmendProtectionLifetimeAllowanceType): JsValue =
        JsString(ltaType.value)
    }

    val jsonReads: Reads[AmendProtectionLifetimeAllowanceType] = new Reads[AmendProtectionLifetimeAllowanceType] {
      override def reads(json: JsValue): JsResult[AmendProtectionLifetimeAllowanceType] =
        json match {
          case JsString(str) =>
            allTypes
              .find(_.value == str)
              .fold[JsResult[AmendProtectionLifetimeAllowanceType]](
                JsError(s"Received unknown LifetimeAllowanceType: $str")
              )(
                JsSuccess(_)
              )

          case other =>
            JsError(s"Cannot create LifetimeAllowanceType instance from: ${other.toString}")
        }
    }

    Format(jsonReads, jsonWrites)
  }

}

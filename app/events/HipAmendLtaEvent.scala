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

package events

import play.api.libs.json.{JsObject, JsString}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.model.DataEvent

class HipAmendLtaEvent(
    nino: String,
    id: Long,
    hipRequestBodyJs: JsObject,
    hipResponseBodyJs: JsObject,
    statusCode: Int,
    path: String
)(implicit hc: HeaderCarrier)
    extends DataEvent(
      auditSource = "pensions-lifetime-allowance",
      auditType = "AmendAllowance",
      detail = Map[String, String](
        "nino"           -> nino,
        "protectionType" -> (hipRequestBodyJs \ "lifetimeAllowanceProtectionRecord" \ "type").as[JsString].value,
        "statusCode"     -> statusCode.toString,
        "protectionStatus" -> (hipResponseBodyJs \ "updatedLifetimeAllowanceProtectionRecord" \ "type")
          .as[JsString]
          .value,
        "protectionId" -> id.toString
      ),
      tags = hc.toAuditTags("amend-pensions-lifetime-allowance", path)
    )

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

package model.hip.existing

import util.{JsonEnum, JsonEnumFormat}

enum ProtectionType(override val jsonString: String) extends JsonEnum {
  case FixedProtection2016          extends ProtectionType("FIXED PROTECTION 2016")
  case IndividualProtection2014     extends ProtectionType("INDIVIDUAL PROTECTION 2014")
  case IndividualProtection2016     extends ProtectionType("INDIVIDUAL PROTECTION 2016")
  case PrimaryProtection            extends ProtectionType("PRIMARY PROTECTION")
  case EnhancedProtection           extends ProtectionType("ENHANCED PROTECTION")
  case FixedProtection              extends ProtectionType("FIXED PROTECTION")
  case FixedProtection2014          extends ProtectionType("FIXED PROTECTION 2014")
  case PensionCreditRights          extends ProtectionType("PENSION CREDIT RIGHTS")
  case InternationalEnhancementS221 extends ProtectionType("INTERNATIONAL ENHANCEMENT (S221)")
  case InternationalEnhancementS224 extends ProtectionType("INTERNATIONAL ENHANCEMENT (S224)")
  case FixedProtection2016LTA       extends ProtectionType("FIXED PROTECTION 2016 LTA")
  case IndividualProtection2014LTA  extends ProtectionType("INDIVIDUAL PROTECTION 2014 LTA")
  case IndividualProtection2016LTA  extends ProtectionType("INDIVIDUAL PROTECTION 2016 LTA")
  case PrimaryProtectionLTA         extends ProtectionType("PRIMARY PROTECTION LTA")
  case EnhancedProtectionLTA        extends ProtectionType("ENHANCED PROTECTION LTA")
  case FixedProtectionLTA           extends ProtectionType("FIXED PROTECTION LTA")
  case FixedProtection2014LTA       extends ProtectionType("FIXED PROTECTION 2014 LTA")
}

object ProtectionType extends JsonEnumFormat[ProtectionType]

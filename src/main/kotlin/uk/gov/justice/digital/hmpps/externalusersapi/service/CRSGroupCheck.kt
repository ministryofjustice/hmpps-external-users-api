package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.springframework.stereotype.Service

@Service
class CRSGroupCheck {
  companion object {
    private val CRS_GROUP_CODE_PREFIXES = listOf("INT_CR_", "INT_SP_")
  }

  fun isCRSGroupCode(groupCode: String) = CRS_GROUP_CODE_PREFIXES.any { groupCode.startsWith(it) }
}

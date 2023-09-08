package uk.gov.justice.digital.hmpps.externalusersapi.util

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.replace

object EmailHelper {
  @JvmStatic
  fun format(emailInput: String?): String? =
    // Single quotes need to be replaced with 2x single quotes to prevent SQLGrammarExceptions. The first single quote is an escape char.
    replace(replace(StringUtils.lowerCase(StringUtils.trim(emailInput)), "'", "''"), "’", "''")
}

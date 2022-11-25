package uk.gov.justice.digital.hmpps.externalusersapi.repository

import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.service.AdminType
import java.lang.String.format
import java.util.regex.Pattern

/**
 Note: This class is not currently used as QueryByExampleExecutor does not yet support paginated queries.
 It could replace RoleFilter when support for pagination becomes available.
 */
class RoleFilterExample(
  roleName: String? = null,
  roleCode: String? = null,
  adminTypes: List<AdminType>? = null
) {

  private val whiteSpace = Pattern.compile("\\s+")
  private var roleByExample: Example<Authority>

  init {
    var roleCodeProbe = ""
    var roleNameProbe = ""
    var adminTypeProbe = ""
    val roleDescription = ""

    val matcher = ExampleMatcher.matchingAll()

    if (!roleName.isNullOrBlank()) {
      roleNameProbe = toLikeString(roleName.trim())
      matcher.withMatcher("roleName", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
    }

    if (!roleCode.isNullOrBlank()) {
      roleCodeProbe = toLikeString(roleCode).uppercase()
      matcher.withMatcher("roleCode", ExampleMatcher.GenericPropertyMatchers.contains())
    }

    adminTypes?.let {
      if (it.isNotEmpty()) {
        adminTypeProbe = buildAdminTypeRegex(it)
        matcher.withMatcher("adminType", ExampleMatcher.GenericPropertyMatchers.regex())
      }
    }

    roleByExample = Example.of(
      Authority(roleCode = roleCodeProbe, roleName = roleNameProbe, roleDescription = roleDescription, adminType = adminTypeProbe), matcher
    )
  }

  private fun buildAdminTypeRegex(adminTypes: List<AdminType>): String {
    val adminTypeRegexStart = "^"
    val adminTypeRegexEnd = ".*$"
    val adminTypeRegex = "(?=.*%s)"

    val regex = StringBuilder()
    regex.append(adminTypeRegexStart)
    for (type in adminTypes) {
      regex.append(format(adminTypeRegex, type.adminTypeCode))
    }

    regex.append(adminTypeRegexEnd)
    return regex.toString()
  }

  private fun toLikeString(input: String): String {
    val words = whiteSpace.split(input.trim())
    return words.joinToString(separator = "% %")
  }
}

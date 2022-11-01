package uk.gov.justice.digital.hmpps.externalusersapi.model.sql

import uk.gov.justice.digital.hmpps.externalusersapi.model.UserFilter
import java.util.regex.Pattern

private const val PROJECTION =
  """
  SELECT
  DISTINCT u.user_id,
  u.email,
  u.enabled,
  u.inactive_reason,
  u.last_logged_in,
  u.locked,
  u.mfa_preference,
  u.password,
  u.password_expiry,
  u.first_name,
  u.last_name,
  u.pre_disable_warning,
  u.source,
  u.username,
  u.verified
  FROM
  users u 
  """

private const val ROLES_JOIN =
  """
  INNER JOIN
  user_role ur
  ON u.user_id = ur.user_id
  INNER JOIN
  roles r
  ON ur.role_id = r.role_id
  """

private const val GROUPS_JOIN =
  """
  INNER JOIN
  user_group ug
  ON u.user_id = ug.user_id
  INNER JOIN
  groups g
  ON ug.group_id = g.group_id
  """

private const val SOURCE_FILTER =
  """
  WHERE
  u.source = 'auth'
  """

private const val ROLES_FILTER =
  """
  AND (r.role_code in :roleCodes)
  """

private const val GROUPS_FILTER =
  """
  AND (g.group_code in :groupCodes)
  """

private const val USER_FILTER =
  """
  AND (
  u.email like :lowerCaseName
  OR u.username like :upperCaseName
  OR lower(concat_ws(' ', u.first_name, u.last_name)) like :lowerCaseName
  OR lower(concat_ws(' ', u.last_name, u.first_name)) like :lowerCaseName
  )
  """

private const val ENABLED_FILTER =
  """
   AND u.enabled = :enabled
  """

private const val ORDER_BY =
  """
  ORDER BY
  u.last_name ASC,
  u.first_name ASC 
  LIMIT ?
  """

class UserFilterEmbedded(
  name: String? = null,
  roleCodes: List<String>? = null,
  groupCodes: List<String>? = null,
  status: UserFilter.Status = UserFilter.Status.ALL,
) {
  private val whiteSpace = Pattern.compile("\\s+")

  private var sql: String = ""
  private var parameters = HashMap<String, String>()

  init {
    val sqlBuilder = StringBuilder()
    sqlBuilder.append(PROJECTION)

    if (!roleCodes.isNullOrEmpty()) {
      sqlBuilder.append(ROLES_JOIN)
    }

    if (!groupCodes.isNullOrEmpty()) {
      sqlBuilder.append(GROUPS_JOIN)
    }

    sqlBuilder.append(SOURCE_FILTER)

    if (!roleCodes.isNullOrEmpty()) {
      sqlBuilder.append(ROLES_FILTER)
      parameters["roleCodes"] = toInString(roleCodes)
    }

    if (!groupCodes.isNullOrEmpty()) {
      sqlBuilder.append(GROUPS_FILTER)
      parameters["groupCodes"] = toInString(groupCodes)
    }

    if (!name.isNullOrBlank()) {
      sqlBuilder.append(USER_FILTER)
      parameters["lowerCaseName"] = toLikeString(name).lowercase()
      parameters["upperCaseName"] = toLikeString(name).uppercase()
    }

    if (status != UserFilter.Status.ALL) {
      sqlBuilder.append(ENABLED_FILTER)
    }

    sqlBuilder.append(ORDER_BY)
    sql = sqlBuilder.toString()
  }

  private fun toInString(parameters: List<String>): String {
    return parameters.joinToString(prefix = "('", postfix = "')", separator = "','")
  }

  private fun toLikeString(input: String): String {
    val words = whiteSpace.split(input.trim())
    return words.joinToString(prefix = "%", postfix = "%", separator = "% %")
  }
}

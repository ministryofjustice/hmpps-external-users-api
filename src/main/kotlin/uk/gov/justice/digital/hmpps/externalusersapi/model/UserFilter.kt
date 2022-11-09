package uk.gov.justice.digital.hmpps.externalusersapi.model

import org.springframework.data.domain.Pageable
import java.lang.String.format
import java.util.regex.Pattern

private const val COUNT_SQL =
  """
    SELECT COUNT(*) AS userCount FROM (%s) AS allUsers
  """

private const val PROJECTION =
  """
  SELECT
  DISTINCT u.user_id,
  u.email,
  u.enabled,
  u.inactive_reason,
  u.last_logged_in,
  u.locked,
  u.first_name,
  u.last_name,
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
  AND (r.role_code in %s)
  """

private const val GROUPS_FILTER =
  """
  AND (g.group_code in %s)
  """

private const val USER_FILTER =
  """
  AND (
  u.email like '%s'
  OR u.username like '%s'
  OR lower(concat_ws(' ', u.first_name, u.last_name)) like '%s'
  OR lower(concat_ws(' ', u.last_name, u.first_name)) like '%s'
  )
  """

private const val ENABLED_FILTER =
  """
   AND u.enabled = %b
  """

private const val ORDER_BY =
  """
  ORDER BY
  u.last_name ASC,
  u.first_name ASC
  """

private const val PAGE_DETAILS =
  """
  LIMIT %d OFFSET %d
  """

class UserFilter(
  name: String? = null,
  roleCodes: List<String>? = null,
  groupCodes: List<String>? = null,
  status: Status = Status.ALL,
  pageable: Pageable
) {
  private val whiteSpace = Pattern.compile("\\s+")

  var sql: String = ""
  var countSQL = ""

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
      sqlBuilder.append(format(ROLES_FILTER, toInString(roleCodes)))
    }

    if (!groupCodes.isNullOrEmpty()) {
      sqlBuilder.append(format(GROUPS_FILTER, toInString(groupCodes)))
    }

    if (!name.isNullOrBlank()) {
      val lowerCaseName = toLikeString(name).lowercase()
      sqlBuilder.append(format(USER_FILTER, lowerCaseName, toLikeString(name).uppercase(), lowerCaseName, lowerCaseName))
    }

    if (status != Status.ALL) {
      sqlBuilder.append(format(ENABLED_FILTER, status == Status.ACTIVE))
    }

    countSQL = format(COUNT_SQL, sqlBuilder.toString())

    sqlBuilder.append(ORDER_BY)
    sqlBuilder.append(format(PAGE_DETAILS, pageable.pageSize, pageable.offset))

    sql = sqlBuilder.toString()
  }

  private fun toInString(parameters: List<String>): String {
    return parameters.joinToString(prefix = "('", postfix = "')", separator = "','")
  }

  private fun toLikeString(input: String): String {
    val words = whiteSpace.split(input.trim())
    return words.joinToString(prefix = "%", postfix = "%", separator = "% %")
  }

  enum class Status {
    ACTIVE, INACTIVE, ALL
  }
}

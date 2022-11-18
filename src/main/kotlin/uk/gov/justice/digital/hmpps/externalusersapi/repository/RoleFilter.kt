package uk.gov.justice.digital.hmpps.externalusersapi.repository

import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import java.lang.String.format
import java.util.Optional
import java.util.regex.Pattern

private const val COUNT_SQL =
  """
    SELECT COUNT(*) AS roleCount FROM (%s) AS allRoles
  """

private const val PROJECTION =
  """
  SELECT
  DISTINCT r.role_id,
           r.admin_type,
           r.role_code,
           r.role_description,
           r.role_name 
  FROM roles r 
  """

private const val FILTER_START =
  """
    WHERE
  """

private const val SUBSEQUENT_FILTER =
  """
    AND 
  """

private const val ROLE_NAME_FILTER =
  """
    (LOWER(r.role_name) like '%s') 
  """

private const val ROLE_CODE_FILTER =
  """
    (r.role_code like '%s') 
  """

private const val ADMIN_TYPE_FILTER =
  """
    (r.admin_type like '%s')
  """

private const val DEFAULT_ORDER_BY =
  """
  ORDER BY r.role_name ASC
  """

private const val ORDER_BY =
  """
  ORDER BY %s %s  
  """

private const val PAGE_DETAILS =
  """
  LIMIT %d OFFSET %d
  """

class RoleFilter(
  roleName: String? = null,
  roleCode: String? = null,
  adminTypes: List<AdminType>? = null,
  pageable: Pageable? = null
) {

  private val sortFieldsMap = hashMapOf(
    "roleName" to "r.role_name",
    "roleCode" to "r.role_code"
  )

  private val whiteSpace = Pattern.compile("\\s+")
  private var filterStarted = false

  var sql: String = ""
  var countSQL = ""

  init {
    val sqlBuilder = StringBuilder(PROJECTION)

    if (!roleName.isNullOrBlank()) {
      sqlBuilder.append(FILTER_START)
      sqlBuilder.append(format(ROLE_NAME_FILTER, toLikeString(roleName.trim())).lowercase())
      filterStarted = true
    }

    if (!roleCode.isNullOrBlank()) {
      appendWhere(sqlBuilder)
      appendAnd(sqlBuilder)
      sqlBuilder.append(format(ROLE_CODE_FILTER, toLikeString(roleCode.trim())).uppercase())
      filterStarted = true
    }

    adminTypes?.let {
      if (it.isNotEmpty()) {
        appendWhere(sqlBuilder)
        adminTypes.forEach { adminType ->
          appendAnd(sqlBuilder)
          sqlBuilder.append(format(ADMIN_TYPE_FILTER, toLikeString(adminType.adminTypeCode)))
          filterStarted = true
        }
      }
    }

    countSQL = format(COUNT_SQL, sqlBuilder.toString())

    pageable?.let {
      val sort = pageable.sort.get().findFirst().unwrap()
      sort?.let {
        val sortField = resolveToSortColumn(sort.property)
        val direction = if (sort.direction.isDescending) "desc" else "asc"
        sqlBuilder.append(format(ORDER_BY, sortField, direction))
      } ?: run {
        sqlBuilder.append(DEFAULT_ORDER_BY)
      }
      sqlBuilder.append(format(PAGE_DETAILS, pageable.pageSize, pageable.offset))
    } ?: run {
      sqlBuilder.append(DEFAULT_ORDER_BY)
    }

    sql = sqlBuilder.toString()
  }

  private fun resolveToSortColumn(fieldName: String): String {
    return if (sortFieldsMap.containsKey(fieldName)) {
      sortFieldsMap[fieldName]!!
    } else {
      "r.role_name"
    }
  }

  private fun <T> Optional<T>.unwrap(): T? = orElse(null)

  private fun appendWhere(sqlBuilder: StringBuilder) {
    if (!filterStarted) {
      sqlBuilder.append(FILTER_START)
    }
  }

  private fun appendAnd(sqlBuilder: StringBuilder) {
    if (filterStarted) {
      sqlBuilder.append(SUBSEQUENT_FILTER)
    }
  }

  private fun toLikeString(input: String): String {
    val words = whiteSpace.split(input.trim())
    return words.joinToString(prefix = "%", postfix = "%", separator = "% %")
  }
}

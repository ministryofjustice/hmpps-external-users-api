package uk.gov.justice.digital.hmpps.externalusersapi.model.sql

import org.mybatis.dynamic.sql.util.kotlin.spring.selectDistinct
import uk.gov.justice.digital.hmpps.externalusersapi.model.UserFilter
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserSqlSupport.email
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserSqlSupport.id
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserSqlSupport.user
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserSqlSupport.userName
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource

class UserFilterSQL(
  val name: String? = null,
  val roleCodes: List<String>? = null,
  val groupCodes: List<String>? = null,
  val status: UserFilter.Status = UserFilter.Status.ALL,
  val authSources: List<AuthSource>? = null,
) {

  fun generateSelect(): String {
    val selectStatement = selectDistinct(id, userName, email) {
      from(user, "usr")

      if (!name.isNullOrBlank()) {
        where {
          email isLike name
        }
        or { userName isLike name }
        // or { firstName. }
      }

      if (!roleCodes.isNullOrEmpty()) {
      }
    }
    return selectStatement.selectStatement
  }
}

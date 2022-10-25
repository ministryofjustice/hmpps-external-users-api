package uk.gov.justice.digital.hmpps.externalusersapi.model.sql

import org.mybatis.dynamic.sql.SqlTable
import java.sql.JDBCType
import java.util.UUID

object UserSqlSupport {
  val user = User()
  val id = user.id
  val userName = user.userName
  val email = user.email
  val firstName = user.firstName
  val lastName = user.lastName

  class User : SqlTable("users") {
    val id = column<UUID>("user_id", JDBCType.JAVA_OBJECT)
    val userName = column<String>("username", JDBCType.VARCHAR)
    val email = column<String>("email", JDBCType.VARCHAR)
    val firstName = column<String>("first_name", JDBCType.VARCHAR)
    val lastName = column<String>("last_name", JDBCType.VARCHAR)
  }
}

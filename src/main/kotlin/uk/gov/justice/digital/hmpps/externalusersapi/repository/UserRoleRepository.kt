package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class UserRoleRepository(private val databaseClient: DatabaseClient) {

  suspend fun deleteUserRole(userId: UUID, roleId: UUID): Long {
    val sql = "delete from user_role " +
      "where " +
      "user_id = :userId " +
      "and role_id = :roleId"

    return databaseClient.sql(sql)
      .bind("userId", userId)
      .bind("roleId", roleId)
      .fetch()
      .rowsUpdated()
      .awaitFirst()
  }

  suspend fun insertUserRole(userId: UUID, roleId: UUID): Long =
    databaseClient
      .sql("INSERT INTO user_role VALUES (:roleId, :userId)")
      .bind("userId", userId)
      .bind("roleId", roleId)
      .fetch()
      .rowsUpdated()
      .awaitFirst()
}

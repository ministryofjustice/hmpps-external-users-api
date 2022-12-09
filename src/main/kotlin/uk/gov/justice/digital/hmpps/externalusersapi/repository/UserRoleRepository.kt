package uk.gov.justice.digital.hmpps.externalusersapi.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.UserRole
import java.util.UUID
import java.util.function.BiFunction

@Repository
class UserRoleRepository(private val databaseClient: DatabaseClient) {

  suspend fun deleteUserRole(userId: UUID, roleId: UUID): Int {
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

  suspend fun insertUserRole(userId: UUID, roleId: UUID): Int =
    databaseClient
      .sql("INSERT INTO user_role VALUES (:roleId, :userId)")
      .bind("userId", userId)
      .bind("roleId", roleId)
      .fetch()
      .rowsUpdated()
      .awaitFirst()

  private val userRoleMappingFunction: BiFunction<Row, RowMetadata, UserRole> =
    BiFunction<Row, RowMetadata, UserRole> { row, _ ->
      UserRole(row.get("user_id", UUID::class.java), row.get("role_id", UUID::class.java))
    }
}

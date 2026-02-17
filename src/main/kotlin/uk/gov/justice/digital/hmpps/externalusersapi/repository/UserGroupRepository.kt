package uk.gov.justice.digital.hmpps.externalusersapi.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitSingleOrNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.UserGroup
import java.util.UUID
import java.util.function.BiFunction

@Repository
class UserGroupRepository(private val databaseClient: DatabaseClient) {

  suspend fun getUserGroup(userId: UUID, groupId: UUID): UserGroup? = databaseClient
    .sql("SELECT * FROM user_group WHERE user_id = :userId and group_id = :groupId")
    .bind("userId", userId)
    .bind("groupId", groupId)
    .map(userGroupMappingFunction)
    .awaitSingleOrNull()

  fun getAllUserGroups(): Flow<UserGroup> = databaseClient
    .sql("SELECT * FROM user_group ")
    .map(userGroupMappingFunction)
    .all().asFlow()

  suspend fun deleteUserGroup(userId: UUID, groupId: UUID): Long {
    val sql = "delete from user_group " +
      "where " +
      "user_id = :userId " +
      "and group_id = :groupId"

    return databaseClient.sql(sql)
      .bind("userId", userId)
      .bind("groupId", groupId)
      .fetch()
      .rowsUpdated()
      .awaitFirst()
  }

  suspend fun insertUserGroup(userId: UUID, groupId: UUID) = databaseClient
    .sql("INSERT INTO user_group VALUES( :groupId, :userId)")
    .bind("userId", userId)
    .bind("groupId", groupId)
    .fetch()
    .rowsUpdated()
    .awaitFirst()

  private val userGroupMappingFunction: BiFunction<Row, RowMetadata, UserGroup> =
    BiFunction<Row, RowMetadata, UserGroup> { row, _ ->
      UserGroup(row.get("user_id", UUID::class.java)!!, row.get("group_id", UUID::class.java)!!)
    }
}

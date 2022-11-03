package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.UserGroup
import java.util.UUID
import java.util.function.BiFunction

@Repository
class UserGroupRepository(private val databaseClient: DatabaseClient) {

  suspend fun getUserGroup(userId: UUID, groupId: UUID): Flow<UserGroup> =
    databaseClient
      .sql("SELECT * FROM user_group WHERE user_id = :userId and group_id = :groupId")
      .bind("userId", userId)
      .bind("groupId", groupId)
      .map(userGroupMappingFunction)
      .all().asFlow()

  private val userGroupMappingFunction: BiFunction<Row, RowMetadata, UserGroup> =
    BiFunction<Row, RowMetadata, UserGroup> { row, _ ->
      UserGroup(row.get("user_id", UUID::class.java), row.get("group_id", UUID::class.java))
    }

  suspend fun deleteUserGroup(userId: UUID, groupId: UUID): Mono<Int> {
    val sql = "delete from user_group " +
      "where " +
      "user_id = :userId " +
      "and group_id = :groupId"

    return databaseClient.sql(sql)
      .bind("userId", userId)
      .bind("groupId", groupId)
      .fetch()
      .rowsUpdated()
  }
}

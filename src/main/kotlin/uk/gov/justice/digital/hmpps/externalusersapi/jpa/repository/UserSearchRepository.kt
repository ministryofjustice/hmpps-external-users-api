package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalusersapi.model.sql.UserFilterSQL
import uk.gov.justice.digital.hmpps.externalusersapi.resource.ExternalUserController.ExternalUser
import java.time.LocalDateTime
import java.util.UUID

@Repository
class UserSearchRepository(private val databaseClient: DatabaseClient) {

  private val externalUserMapper = { row: Row, _: RowMetadata ->
    ExternalUser(
      userId = row.get("user_id", UUID::class.java)?.toString(),
      username = row.get("username", String::class.java),
      email = row.get("email", String::class.java),
      firstName = row.get("first_name", String::class.java),
      lastName = row.get("last_name", String::class.java),
      locked = row.get("locked") as Boolean,
      enabled = row.get("enabled") as Boolean,
      verified = row.get("verified") as Boolean,
      lastLoggedIn = row.get("last_logged_in", LocalDateTime::class.java),
      inactiveReason = row.get("inactive_reason", String::class.java),
    )
  }

  private val countMapper = { row: Row, _: RowMetadata -> row.get("count", Long::class.java) as Long }

  suspend fun searchForUsers(userFilter: UserFilterSQL): Flow<ExternalUser> {
    val query = databaseClient.sql(userFilter.sql)
    return query.map(externalUserMapper).all().asFlow()
  }

  @Suppress("ReactiveStreamsUnusedPublisher")
  suspend fun countAllBy(userFilter: UserFilterSQL): Mono<Long> {
    return databaseClient.sql(userFilter.countSQL)
      .map(countMapper).one()
  }
}

package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalusersapi.model.RoleFilter
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.Authority
import java.util.UUID

@Repository
class RoleSearchRepository(private val databaseClient: DatabaseClient) {

  private val authorityMapper = { row: Row, _: RowMetadata ->
    Authority(
      id = row.get("role_id", UUID::class.java),
      roleCode = row.get("role_code", String::class.java) as String,
      roleName = row.get("role_name", String::class.java) as String,
      roleDescription = row.get("role_description", String::class.java),
      adminType = row.get("admin_type", String::class.java) as String
    )
  }

  private val countMapper = { row: Row, _: RowMetadata -> row.get("roleCount") as Long }

  suspend fun searchForRoles(roleFilter: RoleFilter): Flow<Authority> {
    val query = databaseClient.sql(roleFilter.sql)
    return query.map(authorityMapper).all().asFlow()
  }

  @Suppress("ReactiveStreamsUnusedPublisher")
  suspend fun countAllBy(roleFilter: RoleFilter): Mono<Long> {
    return databaseClient.sql(roleFilter.countSQL)
      .map(countMapper).one()
  }
}

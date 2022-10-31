package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.lang.NonNull
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<User, UUID> {

  @NonNull
  @Query(
    "select * from  USERS u " +
      "  where  " +
      "u.username = :username\n" +
      "and u.source = :source"
  )
  suspend fun findByUsername(username: String?, source: AuthSource): Mono<User>
}

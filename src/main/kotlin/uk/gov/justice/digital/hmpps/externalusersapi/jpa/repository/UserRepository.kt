package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.Optional
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<User, UUID> {
  @Query("select distinct u from User u left join fetch u.authorities where u.username = :username and u.source = :source")
  suspend fun findByUsernameAndSource(username: String?, source: AuthSource): Optional<User>

  suspend fun findByUsername(username: String?): Optional<User> = findByUsernameAndSource(username, AuthSource.auth)
}

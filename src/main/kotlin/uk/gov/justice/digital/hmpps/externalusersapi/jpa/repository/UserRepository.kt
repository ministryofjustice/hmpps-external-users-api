package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface UserRepository : CrudRepository<User, UUID>, JpaSpecificationExecutor<User> {
  @Query("select distinct u from User u left join fetch u.authorities where u.username = :username and u.source = 'auth'")
  fun findByUsernameAndSource(username: String?, source: AuthSource): Optional<User>
  fun findByUsernameAndMasterIsTrue(username: String?): Optional<User> =
    findByUsernameAndSource(username, AuthSource.auth)
}

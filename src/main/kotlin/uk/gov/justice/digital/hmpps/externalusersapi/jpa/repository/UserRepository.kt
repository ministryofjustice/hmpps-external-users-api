package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
import java.util.Optional
import java.util.UUID

interface UserRepository : CrudRepository<User, UUID>, JpaSpecificationExecutor<User> {
  @Query("select distinct u from User u left join fetch u.authorities where u.username = :username")
  fun findByUsername(username: String?): Optional<User>
}

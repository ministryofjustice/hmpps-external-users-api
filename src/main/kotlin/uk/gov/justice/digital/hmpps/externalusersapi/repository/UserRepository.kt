package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.Flow
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
    """ select * from  USERS u
            where
              u.username = :username
                  and
              u.source = :source """
  )
  suspend fun findByUsernameAndSource(username: String?, source: AuthSource = AuthSource.auth): Mono<User>

  @Query(
    "select u.* from USERS u " +
      " inner join user_group ug on u.user_id = ug.user_id " +
      " inner join groups g on ug.group_id = g.group_id " +
      "where " +
      "u.source = :source " +
      "and g.group_code = :groupCode "
  )
  suspend fun findAllByGroupCode(groupCode: String, source: AuthSource = AuthSource.auth): Flow<User>
}

package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<User, UUID> {

  suspend fun findByUsernameAndSource(username: String, source: AuthSource = AuthSource.auth): User?

  @Query(
    """
      select u.* from USERS u 
        inner join user_group ug on u.user_id = ug.user_id 
        inner join groups g on ug.group_id = g.group_id 
      where 
          u.source = :source 
          and 
          g.group_code = :groupCode """
  )
  fun findAllByGroupCode(groupCode: String, source: AuthSource = AuthSource.auth): Flow<User>

  fun findByEmailAndSourceOrderByUsername(email: String?, source: AuthSource = AuthSource.auth): Flow<User>

  fun findByUsernameIn(usernames: List<String>): Flow<User>
}

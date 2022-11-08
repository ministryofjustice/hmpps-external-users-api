package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.lang.NonNull
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import java.util.UUID

interface GroupRepository : CoroutineSortingRepository<Group, String> {
  fun findAllByOrderByGroupName(): Flow<Group>

  suspend fun findByGroupCode(groupCode: String?): Group?

  @NonNull
  @Query(
    """
      select g.* from  groups g
        inner join user_group ug on g.group_id = ug.group_id 
        where
        ug.user_id = :userId
     """
  )
  suspend fun findGroupsByUserId(userId: UUID): Flow<Group>

  @NonNull
  @Query(
    """
      select g.* from groups g
        inner join user_group ug on g.group_id = ug.group_id
        inner join users u on u.user_id = ug.user_id
        where
        u.username = :username
     """
  )
  suspend fun findGroupsByUsername(username: String): Flow<Group>
}

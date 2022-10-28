package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.lang.NonNull
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import java.util.UUID

interface GroupRepository : CoroutineSortingRepository<Group, String> {
  // @Query("Select * from Groups g order by g.group_name")
  fun findAllByOrderByGroupName(): Flow<Group>

  suspend fun findByGroupCode(groupCode: String?): Group?

  @NonNull
  @Query(
    "select g.* from  groups g, " +
      "user_group ug where  " +
      "g.group_id = ug.group_id\n" +
      "and ug.user_id = :userId"
  )
  suspend fun findGroupByUserId(userId: UUID): Flow<Group>
}

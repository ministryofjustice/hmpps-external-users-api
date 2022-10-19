package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group

interface GroupRepository : CoroutineCrudRepository<Group, String> {
  // @Query("Select * from Groups g order by g.group_name")
  fun findAllByOrderByGroupName(): Flow<Group>

  suspend fun findByGroupCode(groupCode: String?): Group?
}

package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.NonNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserAssignableRole

@Repository
interface GroupAssignableRoleRepository : CoroutineSortingRepository<UserAssignableRole, String> {

  @NonNull
  @Query(
    "select distinct  r.role_name,r.role_code,gs.automatic\n" +
      "from group_assignable_role gs, groups g,roles r\n" +
      "where g.group_id = gs.group_id\n" +
      "and r.role_id=gs.role_id and g.group_code = :groupCode"
  )
  suspend fun findGroupAssignableRoleByGroupCode(@Param("groupCode") groupCode: String): Flow<UserAssignableRole>
}

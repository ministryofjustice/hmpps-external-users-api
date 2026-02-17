package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.resource.UserAssignableRoleDto

@Repository
interface GroupAssignableRoleRepository :
  CoroutineCrudRepository<UserAssignableRoleDto, String>,
  CoroutineSortingRepository<UserAssignableRoleDto, String> {

  @Query(
    """
      select distinct  r.role_name,r.role_code, gs.automatic 
       from group_assignable_role gs
         inner join groups g 
            on  g.group_id = gs.group_id 
         inner join roles r 
           on r.role_id=gs.role_id 
          where g.group_code = :groupCode """,
  )
  fun findGroupAssignableRoleByGroupCode(groupCode: String): Flow<UserAssignableRoleDto>
}

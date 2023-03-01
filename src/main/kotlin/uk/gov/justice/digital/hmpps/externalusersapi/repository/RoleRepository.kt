package uk.gov.justice.digital.hmpps.externalusersapi.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.lang.NonNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import java.util.UUID

@Repository
interface RoleRepository : CoroutineSortingRepository<Authority, String> {

  fun findByAdminTypeContainingOrderByRoleName(adminType: String): Flow<Authority>

  suspend fun findByRoleCode(roleCode: String?): Authority?

  @NonNull
  @Query(
    """
      select r.*
      from roles r
        inner join user_role ur on r.role_id = ur.role_id
      where
        ur.user_id = :userId
     """
  )
  fun findRolesByUserId(userId: UUID): Flow<Authority>

  @NonNull
  @Query(
    """
      select distinct r.*
       from group_assignable_role gs
         inner join groups g 
            on g.group_id = gs.group_id 
         inner join roles r 
           on r.role_id = gs.role_id 
          where g.group_code = :groupCode
    """
  )
  fun findRolesByGroupCode(groupCode: String): Flow<Authority>

  @Query(
    """
      select r.*
      from group_assignable_role gar
        inner join groups g on g.group_id = gar.group_id 
        inner join user_group ug on g.group_id = ug.group_id
        inner join users u on u.user_id = ug.user_id
        inner join roles r on r.role_id = gar.role_id 
      where
        u.user_id = :userId order by r.role_name
    """
  )
  fun findByGroupAssignableRolesForUserId(userId: UUID): Flow<Authority>

  @Query(
    """
      select r.*
      from group_assignable_role gar
        inner join groups g on g.group_id = gar.group_id 
        inner join user_group ug on g.group_id = ug.group_id
        inner join users u on u.user_id = ug.user_id
        inner join roles r on r.role_id = gar.role_id 
      where
        u.username = :userName order by r.role_name
    """
  )
  fun findByGroupAssignableRolesForUserName(userName: String): Flow<Authority>

  @Query(
    """
      select r.* from  roles r, user_role ur , USERS u 
      where  r.role_id = ur.role_id and u.user_id = ur.user_id
      and  u.username = :userName
    """
  )
  fun findByUserRolesForUserName(userName: String): Flow<Authority>
}

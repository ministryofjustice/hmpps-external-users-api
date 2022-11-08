package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.NonNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.Authority
import java.util.UUID

@Repository
interface RoleRepository : CoroutineSortingRepository<Authority, String> {
  @Query("select * from roles where admin_type LIKE concat('%', :adminType, '%') order by role_name")
  fun findAllByOrderByRoleNameLike(@Param("adminType") adminType: String): Flow<Authority>

  suspend fun findByRoleCode(roleCode: String?): Authority?

  fun findAllBy(/* RoleFilter, */ pageable: Pageable): Flow<Authority>
  suspend fun countAllBy(/* RoleFilter */): Long

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
  suspend fun findRoleByUserId(userId: UUID): Flow<Authority>

  @NonNull
  @Query(
    """
      select distinct r.*
       from group_assignable_role gs
         inner join groups g 
            on g.group_id = gs.group_id 
         inner join roles r 
           on r.role_id=gs.role_id 
          where g.group_code = :groupCode
    """
  )
  suspend fun findRolesByGroupCode(groupCode: String): Flow<Authority>
}

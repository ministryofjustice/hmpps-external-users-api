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
  @NonNull
  @Query("Select * from Roles r where r.admin_type LIKE %:adminType% order by r.role_name")
  fun findAllByOrderByRoleNameLike(@Param("adminType") adminType: String): Flow<Authority>

  suspend fun findByRoleCode(roleCode: String?): Authority?

  fun findAllBy(/* RoleFilter, */ pageable: Pageable): Flow<Authority>
  suspend fun countAllBy(/* RoleFilter */): Long

  @NonNull
  @Query(
    "select r.* from  roles r, user_role ur " +
      "where  r.role_id = ur.role_id\n" +
      " and ur.user_id = :userId"
  )
  suspend fun findRoleByUserId(userId: UUID): Flow<Authority>
}

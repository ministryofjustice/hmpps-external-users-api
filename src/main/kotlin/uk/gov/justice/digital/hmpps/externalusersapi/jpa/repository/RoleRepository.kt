package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.NonNull
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.Authority

@Repository
interface RoleRepository : CoroutineSortingRepository<Authority, String> {
  @NonNull
  @Query("Select * from Roles r where r.admin_type LIKE %:adminType% order by r.role_name")
  fun findAllByOrderByRoleNameLike(@Param("adminType") adminType: String): Flow<Authority>

  suspend fun findByRoleCode(roleCode: String?): Authority?

  fun findAllBy(/* RoleFilter, */ pageable: Pageable): Flow<Authority>
  suspend fun countAllBy(/* RoleFilter */): Long
}

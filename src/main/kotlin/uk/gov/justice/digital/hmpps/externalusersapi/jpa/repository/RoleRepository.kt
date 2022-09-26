package uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.lang.NonNull
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority

interface RoleRepository : CrudRepository<Authority, String>, JpaSpecificationExecutor<Authority> {
  @NonNull
  @Query("Select * from Roles r where r.admin_type LIKE %:adminType% order by r.role_name", nativeQuery = true)
  fun findAllByOrderByRoleNameLike(@Param("adminType") adminType: String): List<Authority>

  fun findByRoleCode(roleCode: String?): Authority?
}

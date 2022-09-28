package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.RoleFilter

@Service
@Transactional(readOnly = true)
class RoleService(
  private val roleRepository: RoleRepository
) {
  fun getRoles(
    adminTypes: List<AdminType>?,
  ): List<Authority> {
    val rolesFilter = RoleFilter(adminTypes = adminTypes)
    return roleRepository.findAll(rolesFilter, Sort.by(Sort.Direction.ASC, "roleName"))
  }

  fun getRoles(
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?,
    pageable: Pageable,
  ): Page<Authority> {

    val rolesFilter = RoleFilter(
      roleName = roleName,
      adminTypes = adminTypes,
      roleCode = roleCode,
    )

    return roleRepository.findAll(rolesFilter, pageable)
  }

  @Throws(RoleNotFoundException::class)
  fun getRoleDetails(roleCode: String): Authority {
    val role = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("get", roleCode, "notfound")
    Hibernate.initialize(role.adminType)
    return role
  }

  class RoleNotFoundException(val action: String, val roleCode: String, val errorCode: String) :
    Exception("Unable to $action role: $roleCode with reason: $errorCode")
}

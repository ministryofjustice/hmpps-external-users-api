package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.RoleFilter
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleAdminTypeAmendment

@Service
@Transactional(readOnly = true)
class RoleService(
  private val roleRepository: RoleRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade
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

  @Transactional
  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    val roleToUpdate = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("maintain", roleCode, "notfound")
    val immutableAdminTypesInDb = roleToUpdate.adminType.filter { it != AdminType.DPS_LSA }

    roleToUpdate.adminType = (roleAmendment.adminType.addDpsAdmTypeIfRequired() + immutableAdminTypesInDb).toList()
    roleRepository.save(roleToUpdate)
    telemetryClient.trackEvent(
      "RoleAdminTypeUpdateSuccess",
      mapOf("username" to authenticationFacade.currentUsername, "roleCode" to roleCode, "newRoleAdminType" to roleToUpdate.adminType.toString()),
      null
    )
  }

  private fun Set<AdminType>.addDpsAdmTypeIfRequired() = (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this)

  class RoleNotFoundException(val action: String, val roleCode: String, val errorCode: String) :
    Exception("Unable to $action role: $roleCode with reason: $errorCode")
}

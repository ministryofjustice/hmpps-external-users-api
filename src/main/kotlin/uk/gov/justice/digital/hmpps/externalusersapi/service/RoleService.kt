package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.hibernate.Hibernate
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.externalusersapi.model.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.model.RoleFilter
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleNameAmendment

@Service
@Transactional(readOnly = true)
class RoleService(
  private val roleRepository: RoleRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade
) {
  @Transactional
  @Throws(RoleExistsException::class)
  suspend fun createRole(createRole: CreateRole) {
    val roleCode = createRole.roleCode.trim().uppercase()
    val roleFromDb = roleRepository.findByRoleCode(roleCode)
    roleFromDb?.let { throw RoleExistsException(roleCode, "role code already exists") }

    val roleName = createRole.roleName.trim()
    val roleDescription = createRole.roleDescription?.trim()
    val adminType = createRole.adminType.addDpsAdmTypeIfRequired().toList()

    val role =
      Authority(roleCode = roleCode, roleName = roleName, roleDescription = roleDescription, adminType = adminType)
    roleRepository.save(role)

    telemetryClient.trackEvent(
      "RoleCreateSuccess",
      mapOf(
        "username" to authenticationFacade.currentUsername,
        "roleCode" to roleCode,
        "roleName" to roleName,
        "roleDescription" to roleDescription,
        "adminType" to adminType.toString()
      ),
      null
    )
  }

  suspend fun getRoles(
    adminTypes: List<AdminType>?,
  ): List<Authority> {
    val rolesFilter = RoleFilter(adminTypes = adminTypes)
    // TODO Fix this
    // return roleRepository.findAll(rolesFilter, Sort.by(Sort.Direction.ASC, "roleName"))
    return roleRepository.findAll().toList()
  }

  suspend fun getRoles(
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?,
    pageable: Pageable,
  ): Page<Authority> =
    // TODO Check/Text/Fix this
    coroutineScope {
      val rolesFilter = RoleFilter(
        roleName = roleName,
        adminTypes = adminTypes,
        roleCode = roleCode,
      )

      val roles = async {
        roleRepository.findAll(rolesFilter, pageable)
      }

      val count = async {
        roleRepository.countAllByFindAll(rolesFilter)
      }

      PageImpl(
        roles.await().toList().map { it },
        pageable, count.await()
      )
    }

  @Throws(RoleNotFoundException::class)
  suspend fun getRoleDetails(roleCode: String): Authority {
    val role = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("get", roleCode, "notfound")
    Hibernate.initialize(role.adminType)
    return role
  }

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    val roleToUpdate = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("maintain", roleCode, "notfound")

    roleToUpdate.roleName = roleAmendment.roleName
    roleRepository.save(roleToUpdate)

    telemetryClient.trackEvent(
      "RoleNameUpdateSuccess",
      mapOf("username" to authenticationFacade.currentUsername, "roleCode" to roleCode, "newRoleName" to roleAmendment.roleName),
      null
    )
  }

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    val roleToUpdate = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("maintain", roleCode, "notfound")

    roleToUpdate.roleDescription = roleAmendment.roleDescription
    roleRepository.save(roleToUpdate)

    telemetryClient.trackEvent(
      "RoleDescriptionUpdateSuccess",
      mapOf("username" to authenticationFacade.currentUsername, "roleCode" to roleCode, "newRoleDescription" to roleAmendment.roleDescription),
      null
    )
  }

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
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

  class RoleNotFoundException(val action: String, val role: String, val errorCode: String) :
    Exception("Unable to $action role: $role with reason: $errorCode")

  class RoleExistsException(val role: String, val errorCode: String) :
    Exception("Unable to create role: $role with reason: $errorCode")
}

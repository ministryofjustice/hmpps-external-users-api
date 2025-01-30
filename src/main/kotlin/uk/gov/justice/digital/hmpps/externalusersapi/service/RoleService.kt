package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.fasterxml.jackson.annotation.JsonFormat
import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleFilter
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleSearchRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleDescriptionAmendmentDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleDetailsDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.RoleNameAmendmentDto

@Service
@Transactional(readOnly = true)
class RoleService(
  private val roleSearchRepository: RoleSearchRepository,
  private val roleRepository: RoleRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
) {
  @Transactional
  @Throws(RoleExistsException::class)
  suspend fun createRole(createRole: CreateRoleDto) {
    val roleCode = createRole.roleCode.trim().uppercase()
    val roleFromDb = roleRepository.findByRoleCode(roleCode)
    roleFromDb?.let { throw RoleExistsException(roleCode, "role code already exists") }

    val roleName = createRole.roleName.trim()
    val roleDescription = createRole.roleDescription?.trim()
    val adminType = convertAdminTypeListToString(createRole.adminType.addDpsAdmTypeIfRequired().toList())

    val role =
      Authority(roleCode = roleCode, roleName = roleName, roleDescription = roleDescription, adminType = adminType)
    roleRepository.save(role)

    telemetryClient.trackEvent(
      "RoleCreateSuccess",
      mapOf(
        "username" to authenticationFacade.getUsername(),
        "roleCode" to roleCode,
        "roleName" to roleName,
        "roleDescription" to roleDescription,
        "adminType" to adminType,
      ),
      null,
    )
  }

  suspend fun getRoles(adminTypes: List<AdminType>?) = roleSearchRepository.searchForRoles(RoleFilter(adminTypes = adminTypes))

  suspend fun getRoles(
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?,
    pageable: Pageable,
  ): Page<Authority> = coroutineScope {
    val rolesFilter = RoleFilter(
      roleName = roleName,
      adminTypes = adminTypes,
      roleCode = roleCode,
      pageable = pageable,
    )

    val roles = async {
      roleSearchRepository.searchForRoles(rolesFilter)
    }

    val count = async {
      roleSearchRepository.countAllBy(rolesFilter)
    }

    PageImpl(
      roles.await().toList(),
      pageable,
      count.await(),
    )
  }

  @Throws(RoleNotFoundException::class)
  suspend fun getRoleDetails(roleCode: String): RoleDetailsDto = roleRepository.findByRoleCode(roleCode)
    ?.let {
      RoleDetailsDto(it)
    } ?: throw RoleNotFoundException("get", roleCode, "notfound")

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendmentDto) {
    val roleToUpdate = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("maintain", roleCode, "notfound")

    roleToUpdate.roleName = roleAmendment.roleName
    roleRepository.save(roleToUpdate)

    telemetryClient.trackEvent(
      "RoleNameUpdateSuccess",
      mapOf("username" to authenticationFacade.getUsername(), "roleCode" to roleCode, "newRoleName" to roleAmendment.roleName),
      null,
    )
  }

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendmentDto) {
    val roleToUpdate = roleRepository.findByRoleCode(roleCode) ?: throw RoleNotFoundException("maintain", roleCode, "notfound")

    roleToUpdate.roleDescription = roleAmendment.roleDescription
    roleRepository.save(roleToUpdate)

    telemetryClient.trackEvent(
      "RoleDescriptionUpdateSuccess",
      mapOf("username" to authenticationFacade.getUsername(), "roleCode" to roleCode, "newRoleDescription" to roleAmendment.roleDescription),
      null,
    )
  }

  @Transactional
  @Throws(RoleNotFoundException::class)
  suspend fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendmentDto) = roleRepository.findByRoleCode(roleCode)
    ?.let { role ->
      val immutableAdminTypesInDb = immutableTypes(role.adminType)
      val updatedList = (roleAmendment.adminType.addDpsAdmTypeIfRequired() + immutableAdminTypesInDb).toList()

      role.adminType = convertAdminTypeListToString(updatedList)
      roleRepository.save(role)

      telemetryClient.trackEvent(
        "RoleAdminTypeUpdateSuccess",
        mapOf("username" to authenticationFacade.getUsername(), "roleCode" to roleCode, "newRoleAdminType" to role.adminType),
        null,
      )
    }
    ?: throw RoleNotFoundException("maintain", roleCode, "notfound")

  private fun Set<AdminType>.addDpsAdmTypeIfRequired() = (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this)

  private fun convertAdminTypeListToString(stringList: List<AdminType>): String = stringList.joinToString(",") { it.adminTypeCode }

  private fun immutableTypes(adminTypesAsString: String): List<AdminType> = convertStringToAdminTypeList(adminTypesAsString).filter { it != AdminType.DPS_LSA }

  private fun convertStringToAdminTypeList(string: String): List<AdminType> = string.split(",").map {
    it.trim()
    AdminType.valueOf(it)
  }

  class RoleNotFoundException(action: String, role: String, errorCode: String) : Exception("Unable to $action role: $role with reason: $errorCode")

  class RoleExistsException(role: String, errorCode: String) : Exception("Unable to create role: $role with reason: $errorCode")
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AdminType(val adminTypeCode: String, val adminTypeName: String) {
  DPS_LSA("DPS_LSA", "DPS Local System Administrator"),
  DPS_ADM("DPS_ADM", "DPS Central Administrator"),
  EXT_ADM("EXT_ADM", "External Administrator"),
  IMS_HIDDEN("IMS_HIDDEN", "IMS Administrator"),
}

package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.google.common.collect.Sets
import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Authority
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.Companion.canMaintainExternalUsers
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.util.UUID
import java.util.function.Consumer

@Service
@Transactional(readOnly = true)
class UserRoleService(
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val userRoleRepository: UserRoleRepository,
  private val roleRepository: RoleRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
  private val auditService: HmppsAuditService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun canAddAuthClients(authorities: Collection<GrantedAuthority>) = authorities.map { it.authority }
      .any { "ROLE_OAUTH_ADMIN" == it }
  }

  val extAdmRoles: Flow<Authority>
    get() = roleRepository.findByAdminTypeContainingOrderByRoleName(AdminType.EXT_ADM.adminTypeCode)

  val imsRoles: Flow<Authority>
    get() = roleRepository.findByAdminTypeContainingOrderByRoleName(AdminType.IMS_HIDDEN.adminTypeCode)

  suspend fun getUserRoles(userId: UUID) = userRepository.findById(userId)?.let { user: User ->
    // MaintainImsUser doesn't need to check user relationship as is system role
    if (!authenticationFacade.isMaintainImsUser()) {
      maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)
    }
    roleRepository.findRolesByUserId(userId).toList()
  }

  suspend fun getAssignableRolesByUserId(userId: UUID) = userRepository.findById(userId)?.let {
    Sets.difference(
      getAllAssignableRolesByUserId(userId).toSet(),
      roleRepository.findRolesByUserId(userId).toSet(),
    )
      .sortedBy { it.roleName }
  } ?: throw UsernameNotFoundException("User $userId not found")

  suspend fun getAllAssignableRolesByUserId(userId: UUID) = if (canMaintainExternalUsers(authenticationFacade.getAuthentication().authorities)) {
    // only allow oauth admins to see that role
    extAdmRoles.filter { r: Authority -> "OAUTH_ADMIN" != r.roleCode || canAddAuthClients(authenticationFacade.getAuthentication().authorities) }
      .toSet()
    // otherwise they can assign all roles that can be assigned to any of their groups
  } else {
    roleRepository.findByGroupAssignableRolesForUserId(userId).toSet()
  }

  @Transactional
  suspend fun addRolesByUserId(
    userId: UUID,
    roleCodes: List<String>,
  ) {
    // already checked that user exists
    userRepository.findById(userId)?.let { user: User ->

      // MaintainImsUser doesn't need to check user relationship as is system role
      if (!authenticationFacade.isMaintainImsUser()) {
        // check that the logged in user has permission to modify user
        maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)
      }

      val formattedRoles = roleCodes.map { formatRole(it) }
      for (roleCode in formattedRoles) {
        // check that role exists
        val role = roleRepository.findByRoleCode(roleCode) ?: throw UserRoleException("role", "role.notfound")

        val userRoles = roleRepository.findRolesByUserId(userId).toList()

        if (userRoles.contains(role)) {
          throw UserRoleExistsException()
        }
        // get correct roles set
        if (authenticationFacade.isMaintainImsUser()) {
          if (!imsRoles.toSet().contains(role)) {
            throw UserRoleException("role", "invalid")
          }
        } else if (!getAllAssignableRolesByUserId(userId).contains(role)) {
          throw UserRoleException("role", "invalid")
        }
        userRoleRepository.insertUserRole(userId, role.id!!)
      }
      // now that roles have all been added, then audit the role additions
      val maintainerName = authenticationFacade.getUsername()
      formattedRoles.forEach(
        Consumer { roleCode: String ->
          telemetryClient.trackEvent(
            "ExternalUserRoleAddSuccess",
            mapOf(
              "userId" to userId.toString(),
              "username" to user.getUserName(),
              "role" to roleCode,
              "admin" to maintainerName,
            ),
            null,
          )
          log.info("Adding role {} to user {}", roleCode, userId)
        },
      )
      publishAuditEvent(userId, maintainerName, roleCodes)
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  suspend fun getRolesByUsername(username: String): Set<Authority> {
    userRepository.findByUsernameAndSource(username)
      ?: throw UsernameNotFoundException("User with username $username not found")
    return roleRepository.findByUserRolesForUserName(username).toSet()
  }

  @Transactional
  suspend fun removeRoleByUserId(
    userId: UUID,
    roleCode: String,
  ) {
    // already checked that user exists
    userRepository.findById(userId)?.let { user: User ->
      // MaintainImsUser doesn't need to check user relationship as is system role
      if (!authenticationFacade.isMaintainImsUser()) {
        // check that the logged in user has permission to modify user
        maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)
      }

      val roleFormatted = formatRole(roleCode)
      val role = roleRepository.findByRoleCode(roleFormatted) ?: throw UserRoleException("role", "role.notfound")
      val userRoles = roleRepository.findRolesByUserId(userId).toList()

      val userRoleToDelete = userRoles.find { it.roleCode == roleFormatted }
        ?: throw UserRoleException("role", "role.missing")

      // get correct roles set
      if (authenticationFacade.isMaintainImsUser()) {
        if (!imsRoles.toSet().contains(role)) {
          throw UserRoleException("role", "invalid")
        }
      } else if (!getAllAssignableRolesByUserId(userId).contains(role)) {
        throw UserRoleException("role", "invalid")
      }

      log.info("Removing role {} from userId {}", roleFormatted, userId)

      userRoleRepository.deleteUserRole(userId, userRoleToDelete.id!!)

      telemetryClient.trackEvent(
        "ExternalUserRoleRemoveSuccess",
        mapOf(
          "userId" to userId.toString(),
          "username" to user.getUserName(),
          "role" to roleFormatted,
          "admin" to authenticationFacade.getUsername(),
        ),
        null,
      )
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  suspend fun getAllAssignableRoles() = if (canMaintainExternalUsers(authenticationFacade.getAuthentication().authorities)) {
    // only allow oauth admins to see that role
    extAdmRoles.filter { r: Authority -> "OAUTH_ADMIN" != r.roleCode || canAddAuthClients(authenticationFacade.getAuthentication().authorities) }
      .toSet()
    // otherwise they can assign all roles that can be assigned to any of their groups
  } else {
    roleRepository.findByGroupAssignableRolesForUserName(authenticationFacade.getUsername()).toSet()
  }

  private fun formatRole(role: String) = Authority.removeRolePrefixIfNecessary(StringUtils.upperCase(StringUtils.trim(role)))

  open class UserRoleException(val field: String, val errorCode: String) : Exception("Modify role failed for field $field with reason: $errorCode")

  class UserRoleExistsException : UserRoleException("role", "role.exists")

  private suspend fun publishAuditEvent(userId: UUID, maintainerName: String, roleCodes: List<String>) {
    auditService.publishEvent(
      what = "ADD_ROLE",
      who = maintainerName,
      subjectId = userId.toString(),
      subjectType = "USER_ID",
      correlationId = null,
      service = "hmpps-external-users-api",
      details = Json.encodeToString(AddedRoles(roleCodes)),
    )
  }
}

@Serializable
data class AddedRoles(val addedRoles: List<String>)

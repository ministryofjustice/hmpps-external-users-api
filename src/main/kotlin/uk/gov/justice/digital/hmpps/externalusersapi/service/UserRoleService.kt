package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
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
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.Companion.canMaintainUsers
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserRoleService(
  private val userRepository: UserRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val userRoleRepository: UserRoleRepository,
  private val roleRepository: RoleRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)

    private fun canAddAuthClients(authorities: Collection<GrantedAuthority>) = authorities.map { it.authority }
      .any { "ROLE_OAUTH_ADMIN" == it }
  }

  val allRoles: Flow<Authority>
    get() = roleRepository.findByAdminTypeContainingOrderByRoleName(AdminType.EXT_ADM.adminTypeCode)

  suspend fun getUserRoles(userId: UUID) =
    userRepository.findById(userId)?.let { user: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)
      roleRepository.findRolesByUserId(userId).toList()
    }

  suspend fun getAllAssignableRolesByUserId(userId: UUID) =
    if (canMaintainUsers(authenticationFacade.getAuthentication().authorities)) {
      // only allow oauth admins to see that role
      allRoles.filter { r: Authority -> "OAUTH_ADMIN" != r.roleCode || canAddAuthClients(authenticationFacade.getAuthentication().authorities) }.toSet()
      // otherwise they can assign all roles that can be assigned to any of their groups
    } else roleRepository.findByGroupAssignableRolesForUserId(userId).toSet()

  @Transactional
  suspend fun removeRoleByUserId(
    userId: UUID,
    roleCode: String
  ) {
    // already checked that user exists
    // check that the logged in user has permission to modify user
    userRepository.findById(userId)?.let { user: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)

      val roleFormatted = formatRole(roleCode)
      val role = roleRepository.findByRoleCode(roleFormatted) ?: throw UserRoleException("role", "role.notfound")
      val userRoles = getUserRoles(userId) ?: throw NotFoundException("usernotfound")

      val userRoleToDelete = userRoles.find { it.roleCode == roleFormatted }
        ?: throw UserRoleException("role", "role.missing")

      if (!getAllAssignableRolesByUserId(userId).contains(role)) {
        throw UserRoleException("role", "invalid")
      }
      log.info("Removing role {} from userId {}", roleFormatted, userId)

      userRoleRepository.deleteUserRole(userId, userRoleToDelete.id!!)

      telemetryClient.trackEvent(
        "AuthUserRoleRemoveSuccess",
        mapOf("userId" to userId.toString(), "role" to roleFormatted, "admin" to authenticationFacade.getUsername()),
        null
      )
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  private fun formatRole(role: String) =
    Authority.removeRolePrefixIfNecessary(StringUtils.upperCase(StringUtils.trim(role)))

  open class UserRoleException(val field: String, val errorCode: String) :
    Exception("Modify role failed for field $field with reason: $errorCode")
}

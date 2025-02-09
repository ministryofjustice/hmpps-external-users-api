package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserGroupCoroutineRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRoleCoroutineRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.UserGroup
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.UserRole
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateUser
import uk.gov.justice.digital.hmpps.externalusersapi.security.AuthSource
import java.util.UUID

@Service
@Transactional(readOnly = true)
class CreateUserService(
  private val userRepository: UserRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
  private val userGroupService: UserGroupService,
  private val roleRepository: RoleRepository,
  private val userGroupCoroutineRepository: UserGroupCoroutineRepository,
  private val userRoleCoroutineRepository: UserRoleCoroutineRepository,
) {
  @Transactional
  suspend fun createUserByEmail(createUser: CreateUser): UUID? {
    log.info("Creating External User : {} ", createUser.email)
    var user = userRepository.findByUsernameAndSource(StringUtils.upperCase(createUser.email))
    if (user != null) {
      throw UserExistsException("userId", "User with username ${createUser.email} already exists")
    }

    if (userRepository.findByEmailAndSourceOrderByUsername(createUser.email).count() >= 1) {
      throw UserExistsException("email", "Email with username ${createUser.email} already exists")
    }
    val groups = getInitialGroups(createUser.groupCodes)

    user = saveUser(createUser)
    log.info("External User created: {} , generated user id {}", user.name, user.id)
    saveUserGroups(user, groups)
    saveUserRoles(user, groups)

    telemetryClient.trackEvent(
      "ExternalUserCreated",
      mapOf(
        "username" to user.name,
        "admin" to authenticationFacade.getUsername(),
        "groups" to groups.map { it.groupCode }.toString(),
      ),
      null,
    )

    return user.id
  }

  private suspend fun saveUser(createUser: CreateUser): User {
    val user = User(username = StringUtils.upperCase(createUser.email), source = AuthSource.auth)
    user.email = createUser.email
    user.setEnabled(true)
    user.lastName = createUser.lastName
    user.setFirstName(createUser.firstName)
    userRepository.save(user)

    return user
  }
  private suspend fun saveUserGroups(user: User, group: Set<Group>) {
    user.id?.let { userId ->
      val groupIds = group.mapNotNull { it.groupId }.toSet()
      val userGroups = groupIds
        .map {
          UserGroup(userId, it)
        }
        .toList()
        .toSet()

      userGroupCoroutineRepository.saveAll(userGroups).toList()
      log.debug("User groups created: {} for user name {}", groupIds, user.name)
    }
  }

  private suspend fun saveUserRoles(user: User, group: Set<Group>) {
    user.id?.let { userId ->
      val roleIds = group
        .map { it ->
          roleRepository.findRolesByGroupCode(it.groupCode)
            .toList()
            .mapNotNull { it.id }
        }
        .flatten()
        .toSet()

      val userRoles = roleIds.map { UserRole(userId, it) }
      userRoleCoroutineRepository.saveAll(userRoles).toList()
      log.debug("User roles created: {} for user name {}", userRoles.map { it.roleId }.toString(), user.name)
    }
  }
  private suspend fun getInitialGroups(
    groupCodes: Set<String>?,
  ): Set<Group> {
    if (groupCodes.isNullOrEmpty()) {
      return if (authenticationFacade.getAuthentication().authorities.any { it.authority == "ROLE_MAINTAIN_OAUTH_USERS" }) {
        emptySet()
      } else {
        throw CreateUserException("groupCode", "missing")
      }
    }
    val authUserGroups = userGroupService.getAssignableGroups(authenticationFacade.getUsername(), authenticationFacade.getAuthentication().authorities)
    val groups = authUserGroups.filter { it.groupCode in groupCodes }.toSet()
    if (groups.isEmpty()) {
      throw CreateUserException("groupCode", "notfound")
    }
    return groups
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

class CreateUserException(val field: String, val errorCode: String) : Exception("Create user failed for field $field with reason: $errorCode")
class UserExistsException(val field: String, val errorCode: String) : Exception("User already exists, for field $field with reason: $errorCode")

package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.GroupIdentity
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.Companion.canMaintainUsers
import uk.gov.justice.digital.hmpps.externalusersapi.security.UserGroupRelationshipException
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserGroupService(
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val roleRepository: RoleRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val userGroupRepository: UserGroupRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getParentGroups(userId: UUID): List<GroupIdentity> {
    userSecurityCheck(userId)
    return groupRepository.findGroupsByUserId(userId).toList()
  }

  suspend fun getAllGroupsUsingChildGroupsInLieuOfParentGroup(userId: UUID): List<GroupIdentity> {
    userSecurityCheck(userId)
    val groups = groupRepository.findGroupsByUserId(userId).toList()
    val allGroups: MutableList<GroupIdentity> = mutableListOf()

    groups.forEach { group ->
      val childGroups = childGroupRepository.findAllByGroup(group.groupId).toList()
      if (childGroups.isNotEmpty()) {
        childGroups.forEach { childGroup -> allGroups.add(childGroup) }
      } else {
        allGroups.add(group)
      }
    }
    return allGroups
  }

  private suspend fun userSecurityCheck(userId: UUID) {
    userRepository.findById(userId)?.let { u: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(u.name)
    } ?: throw UsernameNotFoundException("User $userId not found")
  }

  @Transactional
  @Throws(
    UserGroupException::class,
    UserGroupManagerException::class,
    UserGroupRelationshipException::class
  )
  suspend fun addGroupByUserId(userId: UUID, groupCode: String) {
    // already checked that user exists
    userRepository.findById(userId)?.let { user ->
      val groupFormatted = formatGroup(groupCode)
      // check that group exists
      val group =
        groupRepository.findByGroupCode(groupFormatted) ?: throw UserGroupException("Add", "group", "notfound")

      val userGroup = groupRepository.findGroupsByUserId(userId)
      if (userGroup.toList().contains(group)) {
        throw UserGroupException("Add", "group", "exists")
      }
      // check that modifier is able to add user to group
      if (!checkGroupModifier(groupCode, authenticationFacade.getAuthentication().authorities, authenticationFacade.getUsername())) {
        throw UserGroupManagerException("Add", "group", "managerNotMember")
      }
      // check that modifier is able to maintain the user
      maintainUserCheck.ensureUserLoggedInUserRelationship(user.name)

      log.info("Adding group {} to userId {}", groupFormatted, userId)
      groupRepository.save(group)
      group.groupId?.let {
        userGroupRepository.insertUserGroup(userId, it)
      }

      roleRepository.saveAll(roleRepository.findRolesByGroupCode(group.groupCode).toList())
      telemetryClient.trackEvent(
        "AuthUserGroupAddSuccess",
        mapOf("userId" to userId.toString(), "group" to groupFormatted, "admin" to authenticationFacade.getUsername()),
        null
      )
    }
  }

  @Transactional
  @Throws(UserGroupException::class, UserGroupManagerException::class, UserLastGroupException::class)
  suspend fun removeGroupByUserId(
    userId: UUID,
    groupCode: String
  ) {

    userRepository.findById(userId)?.let { user ->
      val groupFormatted = formatGroup(groupCode)
      val userGroup = groupRepository.findGroupsByUserId(userId).toList().toMutableSet()
      if (userGroup.map { it.groupCode }.none { it == groupFormatted }
      ) {
        throw UserGroupException("Remove", "group", "missing")
      }
      if (!checkGroupModifier(groupFormatted, authenticationFacade.getAuthentication().authorities, authenticationFacade.getUsername())) {
        throw UserGroupManagerException("delete", "group", "managerNotMember")
      }

      if (userGroup.count() == 1 && !canMaintainUsers(authenticationFacade.getAuthentication().authorities)) {
        throw UserLastGroupException("group", "last")
      }
      log.info("Removing group {} from userId {}", groupFormatted, userId)
      userGroup.map { it }
        .filter { it.groupCode == groupFormatted }
        .map {
          it.groupId?.let {
            it1 ->
            userGroupRepository.deleteUserGroup(userId, it1)
          }
        }
      telemetryClient.trackEvent(
        "UserGroupRemoveSuccess",
        mapOf("userId" to userId.toString(), "group" to groupCode, "admin" to authenticationFacade.getUsername()),
        null
      )
    }
  }

  @Transactional
  @Throws(UserGroupException::class, UserGroupManagerException::class, UserLastGroupException::class)
  suspend fun removeUserGroup(username: String, groupCode: String, modifier: String?, authorities: Collection<GrantedAuthority>) {
    log.debug("Removing user group $groupCode from user $username")
    val groupFormatted = formatGroup(groupCode)
    // already checked that user exists
    val user = userRepository.findByUsernameAndSource(username) ?: throw NotFoundException("User with username $username not found")

    // Get the user's groups
    val groups = groupRepository.findGroupsByUsername(username).toList()
    val groupToRemove = groups.find { it.groupCode == groupFormatted } ?: throw UserGroupException("Remove", "group", "missing")

    if (!checkGroupModifier(groupFormatted, authorities, modifier)) {
      throw UserGroupManagerException("delete", "group", "managerNotMember")
    }

    if (groups.size == 1 && !canMaintainUsers(authorities)) {
      throw UserLastGroupException("group", "last")
    }

    log.info("Removing group {} from user {}", groupFormatted, username)
    userGroupRepository.deleteUserGroup(user.id!!, groupToRemove.groupId!!)

    telemetryClient.trackEvent(
      "ExternalUserGroupRemoveSuccess",
      mapOf("username" to username, "group" to groupCode.trim(), "admin" to modifier),
      null
    )
  }

  private suspend fun checkGroupModifier(
    groupCode: String,
    authorities: Collection<GrantedAuthority>,
    modifier: String?
  ): Boolean {
    return if (canMaintainUsers(authorities)) {
      true
    } else {
      val modifierGroups = getAssignableGroups(modifier, authorities)
      return modifierGroups.map { it.groupCode }.contains(groupCode)
    }
  }

  private fun formatGroup(group: String) = group.trim().uppercase()

  suspend fun getGroupsByUserName(username: String?): Set<Group>? =
    username?.let {
      userRepository.findByUsernameAndSource(username.trim().uppercase())?.let {
        groupRepository.findGroupsByUsername(username.trim().uppercase()).toSet()
      }
    }

  suspend fun getMyAssignableGroups(): List<Group> {
    return getAssignableGroups(authenticationFacade.getUsername(), authenticationFacade.getAuthentication().authorities)
  }

  suspend fun getAssignableGroups(username: String?, authorities: Collection<GrantedAuthority>): List<Group> =
    if (canMaintainUsers(authorities)) groupRepository.findAllByOrderByGroupName().toList()
    else getGroupsByUserName(username)?.sortedBy { it.groupName } ?: listOf()
}

class UserGroupException(action: String = "add", field: String, errorCode: String) :
  Exception("$action group failed for field $field with reason: $errorCode")

class UserGroupManagerException(action: String = "add", field: String, errorCode: String) :
  Exception("$action group failed for field $field with reason: $errorCode")

class UserLastGroupException(field: String, errorCode: String) :
  Exception("remove group failed for field $field with reason: $errorCode")

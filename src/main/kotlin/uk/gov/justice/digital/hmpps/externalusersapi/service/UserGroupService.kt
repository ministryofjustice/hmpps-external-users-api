package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.RoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.or.GroupORModel
import uk.gov.justice.digital.hmpps.externalusersapi.r2dbc.data.User
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck.Companion.canMaintainUsers
import java.util.Optional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserGroupService(
  private val userRepository: UserRepository,
  private val groupRepository: GroupRepository,
  private val maintainUserCheck: MaintainUserCheck,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val userService: UserService,
  private val roleRepository: RoleRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val userGroupRepository: UserGroupRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getGroups(userId: UUID): MutableList<GroupORModel>? =

    userRepository.findById(userId)?.let { u: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(authenticationFacade.getUsername(), authenticationFacade.getAuthentication().authorities, u)

      val group = groupRepository.findGroupsByUserId(userId).toList().toSet()
      var groupORModel: MutableList<GroupORModel> = mutableListOf()
      group?.forEach {
        group ->
        groupORModel.add(GroupORModel(group, childGroupRepository.findAllByGroup(group.groupId).toList().toMutableSet()))
      }
      return groupORModel
    } // ?: throw UsernameNotFoundException("User $userId not found")

  @Transactional
  @Throws(
    UserGroupException::class,
    UserGroupManagerException::class,
    MaintainUserCheck.UserGroupRelationshipException::class
  )
  suspend fun addGroupByUserId(userId: UUID, groupCode: String) {
    // already checked that user exists
    userRepository.findById(userId)?.let { user ->
      val groupFormatted = formatGroup(groupCode)
      // check that group exists
      val group =
        groupRepository.findByGroupCode(groupFormatted) ?: throw UserGroupException("group", "notfound")

      val userGroup = groupRepository.findGroupsByUserId(userId)
      if (userGroup.toList().contains(group)) {
        throw UserGroupException("group", "exists")
      }
      // check that modifier is able to add user to group
      if (!checkGroupModifier(groupCode, authenticationFacade.getAuthentication().authorities, authenticationFacade.getUsername())) {
        throw UserGroupManagerException("Add", "group", "managerNotMember")
      }
      // check that modifier is able to maintain the user
      maintainUserCheck.ensureUserLoggedInUserRelationship(authenticationFacade.getUsername(), authenticationFacade.getAuthentication().authorities, user)

      log.info("Adding group {} to userId {}", groupFormatted, userId)
      groupRepository.save(group)
      group.groupId?.let {
        userGroupRepository.insertUserGroup(userId, it).awaitSingle()
      }

      user.authorities.addAll(roleRepository.findRolesByGroupCode(group.groupCode).toList())
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
        throw UserGroupException("group", "missing")
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
            userGroupRepository.deleteUserGroup(userId, it1).awaitSingle()
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
  suspend fun removeGroup(username: String, groupCode: String, modifier: String?, authorities: Collection<GrantedAuthority>) {
    val groupFormatted = formatGroup(groupCode)
    // already checked that user exists
    val user = userRepository.findByUsernameAndSource(username).awaitSingleOrNull()
    Optional.of(user!!).orElseThrow()
    if (user.groups.map { it.groupCode }.none { it == groupFormatted }
    ) {
      throw UserGroupException("group", "missing")
    }
    if (!checkGroupModifier(groupFormatted, authorities, modifier)) {
      throw UserGroupManagerException("delete", "group", "managerNotMember")
    }

    if (user.groups.count() == 1 && !canMaintainUsers(authorities)) {
      throw UserLastGroupException("group", "last")
    }

    log.info("Removing group {} from user {}", groupFormatted, username)
    user.groups.removeIf { a: Group -> a.groupCode == groupFormatted }
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

  suspend fun getGroupsByUserName(username: String?): Set<Group>? {
    /* TODO
    return user.map { u: User ->
      Hibernate.initialize(u.groups)
      u.groups.forEach { Hibernate.initialize(it.children) }
      u.groups
    }.orElse(null)
     */
    return userService.getUserAndGroupByUserName(username?.trim()?.uppercase())?.groups?.toSet()
  }

  suspend fun getAssignableGroups(username: String?, authorities: Collection<GrantedAuthority>): List<Group> =
    if (canMaintainUsers(authorities)) groupRepository.findAllByOrderByGroupName().toList()
    else getGroupsByUserName(username)?.sortedBy { it.groupName } ?: listOf()
}

class UserGroupException(field: String, errorCode: String) :
  Exception("Add group failed for field $field with reason: $errorCode")

class UserGroupManagerException(action: String = "add", field: String, errorCode: String) :
  Exception("$action group failed for field $field with reason: $errorCode")

class UserLastGroupException(field: String, errorCode: String) :
  Exception("remove group failed for field $field with reason: $errorCode")

package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.model.User
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
  private val authenticationFacade: AuthenticationFacade
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  val allGroups: List<Group>
    get() = groupRepository.findAllByOrderByGroupName()

  fun getGroups(userId: UUID): Set<Group>? =
    userRepository.findByIdOrNull(userId)?.let { u: User ->
      maintainUserCheck.ensureUserLoggedInUserRelationship(authenticationFacade.currentUsername, authenticationFacade.authentication.authorities, u)
      Hibernate.initialize(u.groups)
      u.groups.forEach { Hibernate.initialize(it.children) }
      u.groups
    }

  @Transactional
  @Throws(
    UserGroupException::class,
    UserGroupManagerException::class,
    UserGroupRelationshipException::class
  )
  fun addGroupByUserId(userId: UUID, groupCode: String) {
    // already checked that user exists
    userRepository.findByIdOrNull(userId)?.let { user ->
      val groupFormatted = formatGroup(groupCode)
      // check that group exists
      val group =
        groupRepository.findByGroupCode(groupFormatted) ?: throw UserGroupException("group", "notfound")
      if (user.groups.contains(group)) {
        throw UserGroupException("group", "exists")
      }
      // check that modifier is able to add user to group
      if (!checkGroupModifier(groupCode, authenticationFacade.authentication.authorities, authenticationFacade.currentUsername)) {
        throw UserGroupManagerException("Add", "group", "managerNotMember")
      }
      // check that modifier is able to maintain the user
      maintainUserCheck.ensureUserLoggedInUserRelationship(authenticationFacade.currentUsername, authenticationFacade.authentication.authorities, user)

      log.info("Adding group {} to userId {}", groupFormatted, userId)
      user.groups.add(group)
      user.authorities.addAll(group.assignableRoles.filter { it.automatic }.map { it.role })
      telemetryClient.trackEvent(
        "AuthUserGroupAddSuccess",
        mapOf("userId" to userId.toString(), "group" to groupFormatted, "admin" to authenticationFacade.currentUsername),
        null
      )
    }
  }

  @Transactional
  @Throws(UserGroupException::class, UserGroupManagerException::class, UserLastGroupException::class)
  fun removeGroupByUserId(
    userId: UUID,
    groupCode: String
  ) {
    userRepository.findByIdOrNull(userId)?.let { user ->
      val groupFormatted = formatGroup(groupCode)
      if (user.groups.map { it.groupCode }.none { it == groupFormatted }
      ) {
        throw UserGroupException("group", "missing")
      }
      if (!checkGroupModifier(groupFormatted, authenticationFacade.authentication.authorities, authenticationFacade.currentUsername)) {
        throw UserGroupManagerException("delete", "group", "managerNotMember")
      }

      if (user.groups.count() == 1 && !canMaintainUsers(authenticationFacade.authentication.authorities)) {
        throw UserLastGroupException("group", "last")
      }

      log.info("Removing group {} from userId {}", groupFormatted, userId)
      user.groups.removeIf { a: Group -> a.groupCode == groupFormatted }
      telemetryClient.trackEvent(
        "UserGroupRemoveSuccess",
        mapOf("userId" to userId.toString(), "group" to groupCode, "admin" to authenticationFacade.currentUsername),
        null
      )
    }
  }

  @Transactional
  @Throws(UserGroupException::class, UserGroupManagerException::class, UserLastGroupException::class)
  fun removeGroup(username: String, groupCode: String, modifier: String?, authorities: Collection<GrantedAuthority>) {
    val groupFormatted = formatGroup(groupCode)
    // already checked that user exists
    val user = userRepository.findByUsername(username).orElseThrow()
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

  private fun checkGroupModifier(
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

  fun getGroupsByUserName(username: String?): Set<Group>? {
    val user = userRepository.findByUsername(username?.trim()?.uppercase())
    return user.map { u: User ->
      Hibernate.initialize(u.groups)
      u.groups.forEach { Hibernate.initialize(it.children) }
      u.groups
    }.orElse(null)
  }

  fun getAssignableGroups(username: String?, authorities: Collection<GrantedAuthority>): List<Group> =
    if (canMaintainUsers(authorities)) allGroups.toList()
    else getGroupsByUserName(username)?.sortedBy { it.groupName } ?: listOf()
}

class UserGroupException(field: String, errorCode: String) :
  Exception("Add group failed for field $field with reason: $errorCode")

class UserGroupManagerException(action: String = "add", field: String, errorCode: String) :
  Exception("$action group failed for field $field with reason: $errorCode")

class UserLastGroupException(field: String, errorCode: String) :
  Exception("remove group failed for field $field with reason: $errorCode")

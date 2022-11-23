package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupAssignableRoleRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateGroup
import uk.gov.justice.digital.hmpps.externalusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.externalusersapi.resource.data.GroupDetails
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck

@Service
@Transactional(readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val userRepository: UserRepository,
  private val userGroupService: UserGroupService,
  private val groupAssignableRoleRepository: GroupAssignableRoleRepository,
  private val maintainUserCheck: MaintainUserCheck,
) {

  suspend fun getAllGroups() = groupRepository.findAllByOrderByGroupName()

  @Throws(GroupNotFoundException::class)
  suspend fun getGroupDetail(groupCode: String): GroupDetails = coroutineScope {
    maintainUserCheck.ensureMaintainerGroupRelationship(authenticationFacade.getUsername(), groupCode)

    val group = async { groupRepository.findByGroupCode(groupCode) }
    group.await()?.let {
      val children = async { childGroupRepository.findAllByGroup(it.groupId) }
      val assignableRoles = async { groupAssignableRoleRepository.findGroupAssignableRoleByGroupCode(it.groupCode) }
      GroupDetails(it, children.await().toList(), assignableRoles.await().toList())
    } ?: throw GroupNotFoundException("get", groupCode, "notfound")
  }

  @Transactional
  @Throws(GroupNotFoundException::class)
  suspend fun updateGroup(groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate =
      groupRepository.findByGroupCode(groupCode) ?: throw GroupNotFoundException("maintain", groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    groupRepository.save(groupToUpdate)

    telemetryClient.trackEvent(
      "GroupUpdateSuccess",
      mapOf(
        "username" to authenticationFacade.getUsername(),
        "groupCode" to groupCode,
        "newGroupName" to groupAmendment.groupName
      ),
      null
    )
  }

  @Transactional
  @Throws(GroupExistsException::class)
  suspend fun createGroup(createGroup: CreateGroup) {
    val groupCode = createGroup.groupCode.trim().uppercase()
    val groupFromDb = groupRepository.findByGroupCode(groupCode)
    groupFromDb?.let { throw GroupExistsException(groupCode, "group code already exists") }

    val groupName = createGroup.groupName.trim()
    val group = Group(groupCode = groupCode, groupName = groupName)
    groupRepository.save(group)

    telemetryClient.trackEvent(
      "GroupCreateSuccess",
      mapOf("username" to authenticationFacade.getUsername(), "groupCode" to groupCode, "groupName" to groupName),
      null
    )
  }

  @Transactional
  @Throws(GroupNotFoundException::class, GroupHasChildGroupException::class)
  suspend fun deleteGroup(groupCode: String) {
    return groupRepository.findByGroupCode(groupCode)
      ?.let { group ->
        val children = childGroupRepository.findAllByGroup(group.groupId).toList()

        when {
          children.isEmpty() -> {
            removeUsersFromGroup(
              groupCode,
              authenticationFacade.getUsername(),
              authenticationFacade.getAuthentication().authorities
            )
            groupRepository.delete(group)

            telemetryClient.trackEvent(
              "GroupDeleteSuccess",
              mapOf("username" to authenticationFacade.getUsername(), "groupCode" to groupCode),
              null
            )
          }
          else -> {
            throw GroupHasChildGroupException(groupCode, "child group exists")
          }
        }
      }
      ?: throw GroupNotFoundException("delete", groupCode, "notfound")
  }

  private suspend fun removeUsersFromGroup(groupCode: String, modifier: String?, authorities: Collection<GrantedAuthority>) {
    val usersWithGroup = userRepository.findAllByGroupCode(groupCode).toList()
    usersWithGroup.forEach { userGroupService.removeUserGroup(it.getUserName(), groupCode, modifier, authorities) }
  }
}

class GroupNotFoundException(action: String, group: String, errorCode: String) :
  Exception("Unable to $action group: $group with reason: $errorCode")

class GroupHasChildGroupException(group: String, errorCode: String) :
  Exception("Unable to delete group: $group with reason: $errorCode")

class GroupExistsException(group: String, errorCode: String) :
  Exception("Unable to create group: $group with reason: $errorCode")

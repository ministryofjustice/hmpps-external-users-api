package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.UserRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateGroup
import uk.gov.justice.digital.hmpps.externalusersapi.resource.GroupAmendment

@Service
@Transactional(readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val childGroupRepository: ChildGroupRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
  private val userRepository: UserRepository,
  private val userGroupService: UserGroupService,
) {

  // TODO Check: can we amend this call to work
/*
  val allGroups: List<Group>
    get() = groupRepository.findAllByOrderByGroupName()
*/
  suspend fun getAllGroups(): List<Group> =
    coroutineScope {
      val groups = async {
        groupRepository.findAllByOrderByGroupName()
      }
      groups.await().toList()
    }

  @Throws(GroupNotFoundException::class)
  suspend fun getGroupDetail(groupCode: String): Group {

    return groupRepository.findByGroupCode(groupCode)
      ?.let { it }
      ?: throw GroupNotFoundException("get", groupCode, "notfound")

    // TODO Fix this
/*
      val requestedGroup =
       return groupRepository.findByGroupCode(groupCode) ?: throw GroupNotFoundException("get", groupCode, "notfound")

      Hibernate.initialize(requestedGroup.assignableRoles)
      Hibernate.initialize(requestedGroup.children)
      maintainUserCheck.ensureMaintainerGroupRelationship(authenticationFacade.currentUsername, groupCode)
      return requestedGroup
 */
  }

  @Throws(ChildGroupNotFoundException::class)
  suspend fun getChildGroupDetail(
    groupCode: String,
  ): ChildGroup {
    return childGroupRepository.findByGroupCode(groupCode) ?: throw ChildGroupNotFoundException(groupCode, "notfound")
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
        "username" to authenticationFacade.currentUsername,
        "groupCode" to groupCode,
        "newGroupName" to groupAmendment.groupName
      ),
      null
    )
  }

  @Transactional
  @Throws(ChildGroupNotFoundException::class)
  suspend fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) {
    val groupToUpdate = childGroupRepository.findByGroupCode(groupCode) ?: throw
    ChildGroupNotFoundException(groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    childGroupRepository.save(groupToUpdate)

    telemetryClient.trackEvent(
      "GroupChildUpdateSuccess",
      mapOf(
        "username" to authenticationFacade.currentUsername,
        "childGroupCode" to groupCode,
        "newChildGroupName" to groupAmendment.groupName
      ),
      null
    )
  }

  @Transactional
  @Throws(ChildGroupNotFoundException::class)
  suspend fun deleteChildGroup(groupCode: String) {
    val childGroup = retrieveChildGroup(groupCode)
    childGroupRepository.delete(childGroup)

    telemetryClient.trackEvent(
      "GroupChildDeleteSuccess",
      mapOf("username" to authenticationFacade.currentUsername, "childGroupCode" to groupCode),
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

    //TODO authenticationFacade.currentUsername is null, needs investigation

  /*    telemetryClient.trackEvent(
      "GroupCreateSuccess",
      mapOf("username" to authenticationFacade.currentUsername, "groupCode" to groupCode, "groupName" to groupName),
      null
    )*/
  }

  @Transactional
  @Throws(GroupNotFoundException::class, GroupHasChildGroupException::class)
  suspend fun deleteGroup(groupCode: String) {
    val group =
      groupRepository.findByGroupCode(groupCode) ?: throw GroupNotFoundException("delete", groupCode, "notfound")
    // TODO Fix this
/*
      when {
        group.children.isEmpty() -> {
          removeUsersFromGroup(
            groupCode,
            authenticationFacade.currentUsername,
            authenticationFacade.authentication.authorities
          )
          groupRepository.delete(group)

          telemetryClient.trackEvent(
            "GroupDeleteSuccess",
            mapOf("username" to authenticationFacade.currentUsername, "groupCode" to groupCode),
            null
          )
        }
        else -> {
          throw GroupHasChildGroupException(groupCode, "child group exists")
        }
      }

*/
  }

  private suspend fun retrieveChildGroup(groupCode: String): ChildGroup {
    return childGroupRepository.findByGroupCode(groupCode) ?: throw ChildGroupNotFoundException(groupCode, "notfound")
  }

  // TODO Fix this
  private fun removeUsersFromGroup(groupCode: String, username: String?, authorities: Collection<GrantedAuthority>) {
    //  val usersWithGroup = userRepository.findAll(UserFilter(groupCodes = listOf(groupCode)))
    //  usersWithGroup.forEach { userGroupService.removeGroup(it.getUserName(), groupCode, username, authorities) }
  }

  @Transactional
  @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
  suspend fun createChildGroup(createChildGroup: CreateChildGroup) {
    val groupCode = createChildGroup.groupCode.trim().uppercase()
    val childGroupFromDB = childGroupRepository.findByGroupCode(groupCode)
    if (childGroupFromDB != null) {
      throw ChildGroupExistsException(groupCode, "Child group code already exists")
    }
    val parentGroupCode = createChildGroup.parentGroupCode.trim().uppercase()
    val parentGroupDetails = groupRepository.findByGroupCode(parentGroupCode) ?: throw
    GroupNotFoundException("create", groupCode, "ParentGroupNotFound")

    val groupName = createChildGroup.groupName.trim()
    val child = ChildGroup(groupCode = createChildGroup.groupCode, groupName = groupName)
    child.group = parentGroupDetails

    childGroupRepository.save(child)

    telemetryClient.trackEvent(
      "GroupChildCreateSuccess",
      mapOf(
        "username" to authenticationFacade.currentUsername,
        "groupCode" to parentGroupDetails.groupCode,
        "childGroupCode" to groupCode,
        "childGroupName" to groupName
      ),
      null
    )
  }
}

class GroupNotFoundException(action: String, group: String, errorCode: String) :
  Exception("Unable to $action group: $group with reason: $errorCode")

class GroupHasChildGroupException(group: String, errorCode: String) :
  Exception("Unable to delete group: $group with reason: $errorCode")

class ChildGroupNotFoundException(group: String, errorCode: String) :
  Exception("Unable to get child group: $group with reason: $errorCode")

class ChildGroupExistsException(group: String, errorCode: String) :
  Exception("Unable to create child group: $group with reason: $errorCode")

class GroupExistsException(group: String, errorCode: String) :
  Exception("Unable to create group: $group with reason: $errorCode")

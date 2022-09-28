package uk.gov.justice.digital.hmpps.externalusersapi.service

import org.hibernate.Hibernate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.jpa.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.model.Group
import uk.gov.justice.digital.hmpps.externalusersapi.security.MaintainUserCheck

@Service
@Transactional(readOnly = true)
class GroupsService(
  private val groupRepository: GroupRepository,
  private val maintainUserCheck: MaintainUserCheck
) {

  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(groupCode: String): Group {
    val requestedGroup =
      groupRepository.findByGroupCode(groupCode) ?: throw GroupNotFoundException("get", groupCode, "notfound")

    Hibernate.initialize(requestedGroup.assignableRoles)
    Hibernate.initialize(requestedGroup.children)
    maintainUserCheck.ensureMaintainerGroupRelationship(groupCode)
    return requestedGroup
  }

    /* @Throws(ChildGroupNotFoundException::class)
     fun getChildGroupDetail(
       groupCode: String,
       maintainerName: String,
       authorities: Collection<GrantedAuthority>
     ): ChildGroup {
       return childGroupRepository.findByGroupCode(groupCode) ?: throw
       GroupNotFoundException("get", groupCode, "notfound")
     }

     @Transactional
     @Throws(GroupNotFoundException::class)
     fun updateGroup(username: String, groupCode: String, groupAmendment: GroupAmendment) {
       val groupToUpdate = groupRepository.findByGroupCode(groupCode) ?: throw
       GroupNotFoundException("maintain", groupCode, "notfound")

       groupToUpdate.groupName = groupAmendment.groupName
       groupRepository.save(groupToUpdate)

       telemetryClient.trackEvent(
         "GroupUpdateSuccess",
         mapOf("username" to username, "groupCode" to groupCode, "newGroupName" to groupAmendment.groupName),
         null
       )
     }

     @Transactional
     @Throws(GroupNotFoundException::class, GroupHasChildGroupException::class)
     fun deleteGroup(username: String, groupCode: String, authorities: Collection<GrantedAuthority>) {
       val group = groupRepository.findByGroupCode(groupCode) ?: throw
       GroupNotFoundException("delete", groupCode, "notfound")

       when {
         group.children.isEmpty() -> {
           removeUsersFromGroup(groupCode, username, authorities)
           groupRepository.delete(group)

           telemetryClient.trackEvent(
             "GroupDeleteSuccess",
             mapOf("username" to username, "groupCode" to groupCode),
             null
           )
         }
         else -> {
           throw GroupHasChildGroupException(groupCode, "child group exist")
         }
       }
     }

     private fun removeUsersFromGroup(groupCode: String, username: String, authorities: Collection<GrantedAuthority>) {
       val usersWithGroup = userRepository.findAll(UserFilter(groupCodes = listOf(groupCode)))
       usersWithGroup.forEach { authUserGroupService.removeGroup(it.username, groupCode, username, authorities) }
     }

     @Transactional
     @Throws(ChildGroupNotFoundException::class)
     fun updateChildGroup(username: String, groupCode: String, groupAmendment: GroupAmendment) {
       val groupToUpdate = childGroupRepository.findByGroupCode(groupCode) ?: throw
       GroupNotFoundException("maintain", groupCode, "notfound")

       groupToUpdate.groupName = groupAmendment.groupName
       childGroupRepository.save(groupToUpdate)

       telemetryClient.trackEvent(
         "GroupChildUpdateSuccess",
         mapOf("username" to username, "childGroupCode" to groupCode, "newChildGroupName" to groupAmendment.groupName),
         null
       )
     }

     @Transactional
     @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
     fun createChildGroup(username: String, createChildGroup: CreateChildGroup) {
       val groupCode = createChildGroup.groupCode.trim().uppercase()
       val childGroupFromDB = childGroupRepository.findByGroupCode(groupCode)
       if (childGroupFromDB != null) {
         throw ChildGroupExistsException(groupCode, "group code already exists")
       }
       val parentGroupCode = createChildGroup.parentGroupCode.trim().uppercase()
       val parentGroupDetails = groupRepository.findByGroupCode(parentGroupCode) ?: throw
       GroupNotFoundException("create", parentGroupCode, "ParentGroupNotFound")

       val groupName = createChildGroup.groupName.trim()
       val child = ChildGroup(groupCode = createChildGroup.groupCode, groupName = groupName)
       child.group = parentGroupDetails

       childGroupRepository.save(child)

       telemetryClient.trackEvent(
         "GroupChildCreateSuccess",
         mapOf(
           "username" to username,
           "groupCode" to parentGroupDetails.groupCode,
           "childGroupCode" to groupCode,
           "childGroupName" to groupName
         ),
         null
       )
     }

     @Transactional
     @Throws(ChildGroupNotFoundException::class)
     fun deleteChildGroup(username: String, groupCode: String) {
       childGroupRepository.deleteByGroupCode(groupCode)

       telemetryClient.trackEvent(
         "GroupChildDeleteSuccess",
         mapOf("username" to username, "childGroupCode" to groupCode),
         null
       )
     }

     @Transactional
     @Throws(GroupExistsException::class)
     fun createGroup(username: String, createGroup: CreateGroup) {
       val groupCode = createGroup.groupCode.trim().uppercase()
       val groupFromDb = groupRepository.findByGroupCode(groupCode)
       groupFromDb?.let { throw GroupExistsException(groupCode, "group code already exists") }

       val groupName = createGroup.groupName.trim()
       val group = Group(groupCode = groupCode, groupName = groupName)
       groupRepository.save(group)

       telemetryClient.trackEvent(
         "GroupCreateSuccess",
         mapOf("username" to username, "groupCode" to groupCode, "groupName" to groupName),
         null
       )
     }*/

  class GroupNotFoundException(val action: String, val group: String, val errorCode: String) :
    Exception("Unable to $action group: $group with reason: $errorCode")

  class GroupHasChildGroupException(val group: String, val errorCode: String) :
    Exception("Unable to delete group: $group with reason: $errorCode")

  class ChildGroupNotFoundException(val group: String, val errorCode: String) :
    Exception("Unable to maintain child group: $group with reason: $errorCode")

  class ChildGroupExistsException(val group: String, val errorCode: String) :
    Exception("Unable to create child group: $group with reason: $errorCode")

  class GroupExistsException(val group: String, val errorCode: String) :
    Exception("Unable to create group: $group with reason: $errorCode")
}

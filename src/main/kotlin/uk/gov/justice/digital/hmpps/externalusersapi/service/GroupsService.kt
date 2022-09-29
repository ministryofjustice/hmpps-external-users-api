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

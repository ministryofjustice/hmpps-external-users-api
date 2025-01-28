package uk.gov.justice.digital.hmpps.externalusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.externalusersapi.repository.ChildGroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.GroupRepository
import uk.gov.justice.digital.hmpps.externalusersapi.repository.entity.ChildGroup
import uk.gov.justice.digital.hmpps.externalusersapi.resource.CreateChildGroupDto
import uk.gov.justice.digital.hmpps.externalusersapi.resource.GroupAmendmentDto

@Service
@Transactional(readOnly = true)
class ChildGroupsService(
  private val childGroupRepository: ChildGroupRepository,
  private val groupRepository: GroupRepository,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
) {

  @Throws(ChildGroupNotFoundException::class)
  suspend fun getChildGroupDetail(
    groupCode: String,
  ): ChildGroup = childGroupRepository.findByGroupCode(groupCode) ?: throw ChildGroupNotFoundException(groupCode, "notfound")

  @Transactional
  @Throws(ChildGroupNotFoundException::class)
  suspend fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendmentDto) {
    val groupToUpdate =
      childGroupRepository.findByGroupCode(groupCode) ?: throw ChildGroupNotFoundException(groupCode, "notfound")

    groupToUpdate.groupName = groupAmendment.groupName
    childGroupRepository.save(groupToUpdate)

    telemetryClient.trackEvent(
      "GroupChildUpdateSuccess",
      mapOf(
        "username" to authenticationFacade.getUsername(),
        "childGroupCode" to groupCode,
        "newChildGroupName" to groupAmendment.groupName,
      ),
      null,
    )
  }

  @Transactional
  @Throws(ChildGroupNotFoundException::class)
  suspend fun deleteChildGroup(groupCode: String) {
    retrieveChildGroup(groupCode)
    childGroupRepository.deleteByGroupCode(groupCode)

    telemetryClient.trackEvent(
      "GroupChildDeleteSuccess",
      mapOf("username" to authenticationFacade.getUsername(), "childGroupCode" to groupCode),
      null,
    )
  }

  @Transactional
  @Throws(ChildGroupExistsException::class, GroupNotFoundException::class)
  suspend fun createChildGroup(createChildGroup: CreateChildGroupDto) {
    val groupCode = createChildGroup.groupCode.trim().uppercase()
    val childGroupFromDB = childGroupRepository.findByGroupCode(groupCode)
    if (childGroupFromDB != null) {
      throw ChildGroupExistsException(groupCode, "Child group code already exists")
    }
    val parentGroupCode = createChildGroup.parentGroupCode.trim().uppercase()
    val parentGroupDetails = groupRepository.findByGroupCode(parentGroupCode) ?: throw
      GroupNotFoundException("create", groupCode, "ParentGroupNotFound")

    val groupName = createChildGroup.groupName.trim()
    val child = ChildGroup(groupCode = createChildGroup.groupCode, groupName = groupName, group = parentGroupDetails.groupId)
    childGroupRepository.save(child)

    telemetryClient.trackEvent(
      "GroupChildCreateSuccess",
      mapOf(
        "username" to authenticationFacade.getUsername(),
        "groupCode" to parentGroupDetails.groupCode,
        "childGroupCode" to groupCode,
        "childGroupName" to groupName,
      ),
      null,
    )
  }

  private suspend fun retrieveChildGroup(groupCode: String): ChildGroup = childGroupRepository.findByGroupCode(groupCode) ?: throw ChildGroupNotFoundException(groupCode, "notfound")
}

class ChildGroupNotFoundException(group: String, errorCode: String) : Exception("Unable to get child group: $group with reason: $errorCode")

class ChildGroupExistsException(group: String, errorCode: String) : Exception("Unable to create child group: $group with reason: $errorCode")
